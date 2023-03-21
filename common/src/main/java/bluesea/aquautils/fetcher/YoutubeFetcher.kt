package bluesea.aquautils.fetcher

import bluesea.aquautils.common.Controller
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.jsoup.Jsoup

// Refer to https://github.com/YuutaTsubasa/ChatroomFetcher
class YoutubeFetcher constructor(allPlayers: Audience, liveId: String) {
    private val _liveId: String
    private val _liveChatApiUri: String
    private val allPlayers: Audience

    companion object {
        private const val LIVE_CHAT_API_URI_FORMAT = "https://www.youtube.com/live_chat?is_popout=1&v=%s"
        private const val GET_LIVE_CHAT_API_URI_FORMAT = "https://www.youtube.com/youtubei/v1/live_chat/get_live_chat?key=%s&prettyPrint=false"
        private const val PAID_TEXT_REGEX_PATTERN = """([^0-9]*)([0-9,.]+)"""

        private val gson = GsonBuilder().disableHtmlEscaping().create()

        var Comments = emptyArray<JsonElement>()
    }

    init {
        this.allPlayers = allPlayers
        _liveId = liveId
        _liveChatApiUri = String.format(LIVE_CHAT_API_URI_FORMAT, liveId)
    }

    private enum class RendererType {
        LiveChatTextMessageRenderer,
        LiveChatPaidMessageRenderer,
        LiveChatPaidStickerRenderer,
        LiveChatMembershipItemRenderer,
        LiveChatSponsorshipsGiftPurchaseAnnouncementRenderer,
        LiveChatSponsorshipsGiftRedemptionAnnouncementRenderer
    }

    @Synchronized
    fun fetch() {
        if (_liveId.isEmpty()) return
        try {
            val httpConnection = Jsoup.connect(_liveChatApiUri)
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
            val clientData = gson
                .fromJson(innerTubeContext["client"], YoutubeChatRequestData.ClientData::class.java)
            clientData.acceptHeader =
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
            clientData.connectionType = "CONN_CELLULAR_4G"
            clientData.MainAppWebInfo = YoutubeChatRequestData.MainAppWebInfoData(
                graftUrl = _liveChatApiUri,
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
            val ytInitialDataJsonObject = gson
                .fromJson(ytInitialDataJsonContent, JsonElement::class.java)

            val continuation = (
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
            Comments.plus(initialComments)

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

            Controller.LOGGER.info("Preset done!")
            initialComments.forEach { comment ->
                allPlayers.sendMessage(GsonComponentSerializer.gson().deserializeFromTree(comment))
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
                    requestData.Continuation = (
                        contentJsonObject
                            ["continuationContents"].asJsonObject
                            ["liveChatContinuation"].asJsonObject
                            ["continuations"].asJsonArray[0].asJsonObject
                            ["invalidationContinuationData"].asJsonObject
                            ["continuation"].asString
                        )

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
                        Comments = Comments.plus(comments)

                        comments.forEach { comment ->
                            allPlayers.sendMessage(GsonComponentSerializer.gson().deserializeFromTree(comment))
                        }
                    }
                } catch (_: NullPointerException) {
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
        allPlayers.sendMessage(
            Component.empty()
                .append(Component.text("[YTChat] ").color(NamedTextColor.RED))
                .append(Component.text("已關閉!"))
        )
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
                    RendererType.LiveChatTextMessageRenderer,
                    existedUserIds,
                    liveChatTextMessageRenderer
                )
            } else if (liveChatPaidMessageRenderer != null) {
                _ConvertToComment(
                    RendererType.LiveChatPaidMessageRenderer,
                    existedUserIds,
                    liveChatPaidMessageRenderer
                )
            } else if (liveChatPaidStickerRenderer != null) {
                _ConvertToComment(
                    RendererType.LiveChatPaidStickerRenderer,
                    existedUserIds,
                    liveChatPaidStickerRenderer
                )
            } else if (liveChatMembershipItemRenderer != null) {
                _ConvertToComment(
                    RendererType.LiveChatMembershipItemRenderer,
                    existedUserIds,
                    liveChatMembershipItemRenderer
                )
            } else if (liveChatSponsorshipsGiftPurchaseAnnouncementRenderer != null) {
                _ConvertToComment(
                    RendererType.LiveChatSponsorshipsGiftPurchaseAnnouncementRenderer,
                    existedUserIds,
                    liveChatSponsorshipsGiftPurchaseAnnouncementRenderer
                )
            } else if (liveChatSponsorshipsGiftRedemptionAnnouncementRenderer != null) {
                _ConvertToComment(
                    RendererType.LiveChatSponsorshipsGiftRedemptionAnnouncementRenderer,
                    existedUserIds,
                    liveChatSponsorshipsGiftRedemptionAnnouncementRenderer
                )
            } else {
                null
            }
        }
    }

    @Suppress("FunctionName")
    private fun _ConvertRunNode(runNode: JsonElement): String =
        if (runNode.asJsonObject.has("emoji")) {
            runNode.asJsonObject["emoji"].asJsonObject["shortcuts"].asJsonArray[0].asString
        } else {
            _BoldText(runNode, _ItalicText(runNode, runNode.asJsonObject["text"].asString))
        }

    @Suppress("FunctionName")
    private fun _DecorateText(runNode: JsonElement, nodeName: String, htmlName: String, text: String): String =
        if (runNode.asJsonObject.has(nodeName) && runNode.asJsonObject[nodeName].asBoolean) {
            "<$htmlName>$text</$htmlName>"
        } else {
            text
        }

    @Suppress("FunctionName")
    private fun _BoldText(runNode: JsonElement, text: String): String =
        _DecorateText(runNode, "bold", "strong", text)

    @Suppress("FunctionName")
    private fun _ItalicText(runNode: JsonElement, text: String): String =
        _DecorateText(runNode, "italic", "em", text)

    @Suppress("FunctionName")
    private fun _ConvertToComment(rendererType: RendererType, existedUserIds: HashSet<String>, renderer: JsonElement): JsonElement {
        val name = renderer.asJsonObject["authorName"].asJsonObject["simpleText"].asString
        val paidText =
            if ((renderer.asJsonObject["purchaseAmountText"] != null)) {
                renderer.asJsonObject["purchaseAmountText"].asJsonObject["simpleText"].asString
            } else {
                ""
            }
        val comment =
            renderer.asJsonObject["message"].asJsonObject["runs"].asJsonArray.joinToString("") { _ConvertRunNode(it) }
                .ifEmpty {
                    renderer.asJsonObject["headerSubtext"].asJsonObject["runs"].asJsonArray.joinToString("") { _ConvertRunNode(it) }
                        .ifEmpty {
                            "<img src=\"${renderer.asJsonObject["sticker"].asJsonObject["thumbnails"].asJsonArray[0].asJsonObject["url"].asString}\"/>"
                        }
                }
        val userId = "yt-${renderer.asJsonObject["authorExternalChannelId"].asString}"
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

        // if (paidText.isNotEmpty()) {
        //     val paidTextRegex = Regex(PAID_TEXT_REGEX_PATTERN)
        //     val paidTextMatches = paidTextRegex.findAll(paidText)
        //
        //     // val colorKeys = new Dictionary<string, string[]> {
        //     //     {"headerBackgroundColor", new [] {"headerBackgroundColor", "moneyChipBackgroundColor"}},
        //     //     {"headerTextColor", new [] {"headerTextColor", "moneyChipTextColor"}},
        //     //     {"bodyBackgroundColor", new [] {"bodyBackgroundColor", "backgroundColor"}},
        //     //     {"bodyTextColor", new [] {"bodyTextColor", "moneyChipTextColor"}},
        //     //     {"authorNameTextColor", new [] {"authorNameTextColor"}},
        //     //     {"timestampColor", new [] {"timestampColor"}}
        //     // }
        //
        //     result.asJsonArray.add(paidText)
        //     result.asJsonArray.add(if (paidTextMatches.count() > 0) paidTextMatches.first().groupValues[0] else "")
        //     result.asJsonArray.add(if (paidTextMatches.count() > 0) paidTextMatches.first().groupValues[1] else "")
        //     // result["data"].asJsonObject.addProperty(
        //     //     "colors", new JsonObject(colorKeys
        //     //     .Where(keyGroup => keyGroup.Value.Any(key => renderer[key] != null))
        //     //     .Select(keyGroup => (Key: keyGroup.Key, Value: keyGroup.Value.First(key => renderer[key] != null)))
        //     //     .Select(keyGroup =>
        //     //     new KeyValuePair<string, JsonNode?>(keyGroup.Key,
        //     //     _ToCssColorString(Color.FromArgb(unchecked((int) renderer[keyGroup.Value].GetValue<uint>()))))))
        //     // )
        // }

        return result
    }
}
