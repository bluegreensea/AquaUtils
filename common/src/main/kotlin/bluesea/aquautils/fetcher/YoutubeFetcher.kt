package bluesea.aquautils.fetcher

import bluesea.aquautils.common.CommonAudienceProvider
import bluesea.aquautils.common.Controller
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import java.awt.Color
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.jsoup.Jsoup

// Refer to https://github.com/YuutaTsubasa/ChatroomFetcher
class YoutubeFetcher(private val provider: CommonAudienceProvider<*, *>, val liveId: String, var sendMsgs: Boolean) {
    private val liveChatApiUri = String.format(LIVE_CHAT_API_URI_FORMAT, liveId)
    val chatLooper = Thread {
        fetch()
    }

    companion object {
        const val WATCH_URI = "https://www.youtube.com/watch?v="
        private const val LIVE_CHAT_API_URI_FORMAT = "https://www.youtube.com/live_chat?is_popout=1&v=%s"
        private const val GET_LIVE_CHAT_API_URI_FORMAT = "https://www.youtube.com/youtubei/v1/live_chat/get_live_chat?key=%s&prettyPrint=false"

        private val gson = GsonBuilder().disableHtmlEscaping().create()

        var allComments = emptyArray<JsonElement>()
    }

    @Synchronized
    fun fetch() {
        if (liveId.isEmpty()) return
        try {
            val httpConnection = Jsoup.connect(liveChatApiUri)
                .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36 Edg/110.0.1587.50")
                .header("origin", "https://www.youtube.com")

            val htmlDocument = httpConnection.get()

            val contextNode = htmlDocument
                .getElementsByTag("script")
                .dataNodes().first { it.wholeData.contains("INNERTUBE_CONTEXT") }
            val extractYtConfigJsonRegEx = Regex("""ytcfg\.set\((.+)\);""")
            val ytConfigJsonContent = extractYtConfigJsonRegEx.find(contextNode.wholeData)!!
                .groupValues[1]
            val ytCfgJsonObject = gson.fromJson(ytConfigJsonContent, JsonElement::class.java)

            val apiKey = ytCfgJsonObject.asJsonObject["INNERTUBE_API_KEY"].asString
            val innerTubeContext = ytCfgJsonObject.asJsonObject["INNERTUBE_CONTEXT"].asJsonObject
            val clientData = gson.fromJson(innerTubeContext["client"], YoutubeChatRequestData.ClientData::class.java)
            clientData.acceptHeader =
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
            clientData.connectionType = "CONN_CELLULAR_4G"
            clientData.MainAppWebInfo = YoutubeChatRequestData.MainAppWebInfoData(
                graftUrl = liveChatApiUri,
                isWebNativeShareAvailable = true,
                webDisplayMode = "WEB_DISPLAY_MODE_BROWSER"
            )
            clientData.memoryTotalKbytes = "8000000"
            clientData.screenDensityFloat = 1.1f
            clientData.screenHeightPoints = 845
            clientData.screenPixelDensity = 1
            clientData.screenWidthPoints = 529
            clientData.timeZone = "Asia/Taipei"
            clientData.userInterfaceTheme = "USER_INTERFACE_THEME_LIGHT"
            clientData.utcOffsetMinutes = 480

            val ytInitialData = htmlDocument
                .getElementsByTag("script")
                .dataNodes().first { it.wholeData.contains("window[\"ytInitialData\"]") }
            val extractYtInitialDataJsonRegEx = Regex("""window\["ytInitialData"] += +(\{.+});\s*""")
            val ytInitialDataJsonContent = extractYtInitialDataJsonRegEx
                .find(ytInitialData.wholeData)!!
                .groupValues[1]
            val ytInitialDataJsonObject = gson.fromJson(ytInitialDataJsonContent, JsonElement::class.java)

            var continuation = (
                ytInitialDataJsonObject.asJsonObject
                    ["contents"].asJsonObject
                    ["liveChatRenderer"].asJsonObject
                    ["continuations"].asJsonArray[0].asJsonObject
                    ["invalidationContinuationData"].asJsonObject
                    ["continuation"].asString
                )

            val existedUserIds = HashSet<String>()
            val initialComments = _ConvertToComments(
                existedUserIds,
                (
                    ytInitialDataJsonObject.asJsonObject
                        ["contents"].asJsonObject
                        ["liveChatRenderer"].asJsonObject
                        ["actions"].asJsonArray
                        .filter { it.asJsonObject.has("addChatItemAction") }
                        .map { it.asJsonObject["addChatItemAction"].asJsonObject["item"] }
                    )
            )
            allComments.plus(initialComments)

            val requestData = YoutubeChatRequestData.YoutubeChatRequestData(
                Context = YoutubeChatRequestData.ContextData(
                    client = clientData,
                    request = YoutubeChatRequestData.RequestData(
                        useSsl = true,
                        consistencyTokenJars = emptyArray(),
                        internalExperimentFlags = emptyArray()
                    ),
                    User = YoutubeChatRequestData.UserData(
                        lockedSafetyMode = false
                    )
                ),
                Continuation = continuation,
                WebClientInfo = YoutubeChatRequestData.WebClientInfoData(
                    isDocumentHidden = false
                )
            )

            Controller.serverLinks["vytchat"] = Pair(Component.text("直播連結"), "$WATCH_URI$liveId")
            provider.getAllPlayers().setServerLinks(Controller.getServerLinks())
            Controller.LOGGER.info("Preset done!")
            if (sendMsgs) {
                initialComments.forEach { comment ->
                    provider.getAllPlayers().sendMessage(GsonComponentSerializer.gson().deserializeFromTree(comment))
                }
            } else {
                sendMsgs = true
            }

            val getLiveChatApiUri = String.format(GET_LIVE_CHAT_API_URI_FORMAT, apiKey)
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val result = httpConnection.url(getLiveChatApiUri)
                        .requestBody(gson.toJson(requestData))
                        .header("Content-Type", "application/json")
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .post()

                    val contentText = result.text()
                    val contentJsonObject = gson.fromJson(contentText, JsonElement::class.java).asJsonObject
                    try {
                        continuation = (
                            contentJsonObject
                                ["continuationContents"].asJsonObject
                                ["liveChatContinuation"].asJsonObject
                                ["continuations"].asJsonArray[0].asJsonObject
                                ["invalidationContinuationData"].asJsonObject
                                ["continuation"].asString
                            )
                        requestData.Continuation = continuation

                        val itemsObject = (
                            contentJsonObject
                                ["continuationContents"].asJsonObject
                                ["liveChatContinuation"].asJsonObject
                            ["actions"]
                            )
                        if (itemsObject != null) {
                            val items = itemsObject.asJsonArray
                                .filter { it.asJsonObject.has("addChatItemAction") }
                                .map { it.asJsonObject["addChatItemAction"].asJsonObject["item"] }
                            val comments = _ConvertToComments(existedUserIds, items)
                            allComments = allComments.plus(comments)

                            comments.forEach { comment ->
                                provider.getAllPlayers().sendMessage(GsonComponentSerializer.gson().deserializeFromTree(comment))
                            }
                        }
                    } catch (e: NullPointerException) {
                        e.printStackTrace()
                        Controller.LOGGER.error("result: $contentText")
                        try {
                            val errorCode = (
                                contentJsonObject
                                    ["error"].asJsonObject
                                    ["code"].asInt
                                )
                            if (errorCode != 500 && errorCode != 503) {
                                break
                            }
                        } catch (_: NullPointerException) {
                            break
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Thread.sleep(7000)
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (Controller.serverLinks.containsKey("vytchat")) {
            Controller.serverLinks.remove("vytchat")
            provider.getAllPlayers().setServerLinks(Controller.getServerLinks())
        }
        if (sendMsgs) {
            provider.getAllPlayers().sendMessage(
                Component.empty()
                    .append(Component.text("[YTChat] ").color(NamedTextColor.RED))
                    .append(Component.text("已關閉!"))
            )
        }
    }

    @Suppress("FunctionName")
    private fun _ConvertToComments(existedUserIds: HashSet<String>, items: List<JsonElement>): List<JsonElement> {
        return items.mapNotNull {
            val item = it.asJsonObject

            val liveChatTextMessageRenderer = item["liveChatTextMessageRenderer"]
            val liveChatPaidMessageRenderer = item["liveChatPaidMessageRenderer"]
            val liveChatPaidStickerRenderer = item["liveChatPaidStickerRenderer"]
            val liveChatMembershipItemRenderer = item["liveChatMembershipItemRenderer"]
            val liveChatSponsorshipsGiftPurchaseAnnouncementRenderer =
                item["liveChatSponsorshipsGiftPurchaseAnnouncementRenderer"]
            val liveChatSponsorshipsGiftRedemptionAnnouncementRenderer =
                item["liveChatSponsorshipsGiftRedemptionAnnouncementRenderer"]

            if (liveChatTextMessageRenderer != null) {
                _ConvertToComment(
                    existedUserIds,
                    liveChatTextMessageRenderer
                )
            } else if (liveChatPaidMessageRenderer != null) {
                _ConvertToComment(
                    existedUserIds,
                    liveChatPaidMessageRenderer
                )
            } else if (liveChatPaidStickerRenderer != null) {
                _ConvertToComment(
                    existedUserIds,
                    liveChatPaidStickerRenderer
                )
            } else if (liveChatMembershipItemRenderer != null) {
                _ConvertToComment(
                    existedUserIds,
                    liveChatMembershipItemRenderer
                )
            } else if (liveChatSponsorshipsGiftPurchaseAnnouncementRenderer != null) {
                _ConvertToComment(
                    existedUserIds,
                    liveChatSponsorshipsGiftPurchaseAnnouncementRenderer
                )
            } else if (liveChatSponsorshipsGiftRedemptionAnnouncementRenderer != null) {
                _ConvertToComment(
                    existedUserIds,
                    liveChatSponsorshipsGiftRedemptionAnnouncementRenderer
                )
            } else {
                null
            }
        }
    }

    @Suppress("FunctionName")
    private fun _ConvertRunNode(runNode: JsonElement): String {
        return if (runNode.asJsonObject.has("emoji")) {
            runNode.asJsonObject["emoji"].asJsonObject["shortcuts"].asJsonArray[0].asString
        } else {
            runNode.asJsonObject["text"].asString
        }
    }

    @Suppress("FunctionName")
    private fun _ConvertToComment(existedUserIds: HashSet<String>, renderer: JsonElement): JsonElement {
        val rendererJsonObject = renderer.asJsonObject

        val name = rendererJsonObject["authorName"].asJsonObject["simpleText"].asString
        val paidText =
            if ((rendererJsonObject["purchaseAmountText"] != null)) {
                rendererJsonObject["purchaseAmountText"].asJsonObject["simpleText"].asString
            } else {
                ""
            }
        val comment =
            rendererJsonObject["message"].asJsonObject["runs"].asJsonArray.joinToString("") { _ConvertRunNode(it) }
                .ifEmpty {
                    rendererJsonObject["headerSubtext"].asJsonObject["runs"].asJsonArray.joinToString("") { _ConvertRunNode(it) }
                        .ifEmpty {
                            "<img src=\"${rendererJsonObject["sticker"].asJsonObject["thumbnails"].asJsonArray[0].asJsonObject["url"].asString}\"/>"
                        }
                }
        val userId = "yt-${rendererJsonObject["authorExternalChannelId"].asString}"
        existedUserIds.add(userId)

        val result = gson.toJsonTree(
            arrayListOf(
                hashMapOf(
                    "text" to name,
                    "color" to "red"
                ),
                hashMapOf(
                    "text" to " $paidText".ifBlank { "" },
                    "color" to "white"
                ),
                hashMapOf(
                    "text" to ": ",
                    "color" to "gray"
                ),
                hashMapOf(
                    "text" to comment,
                    "color" to "white"
                )
            )
        )

        if (paidText.isNotBlank()) {
            val color = Color(rendererJsonObject["bodyBackgroundColor"].asInt)
            val red = color.red
            val green = color.green
            val blue = color.blue
            result.asJsonArray[1].asJsonObject.addProperty(
                "color",
                String.format("#%02x%02x%02x", red, green, blue)
            )
        }

        return result
    }
}
