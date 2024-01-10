package bluesea.aquautils.fetcher

import com.google.gson.annotations.SerializedName

class YoutubeChatRequestData {
    data class UserData(
        @SerializedName("lockedSafetyMode") var lockedSafetyMode: Boolean
    )

    data class RequestData(
        @SerializedName("consistencyTokenJars") var consistencyTokenJars: Array<String>,
        @SerializedName("internalExperimentFlags") var internalExperimentFlags: Array<String>,
        @SerializedName("useSsl") var useSsl: Boolean
    )

    data class MainAppWebInfoData(
        @SerializedName("graftUrl") var graftUrl: String,
        @SerializedName("isWebNativeShareAvailable") var isWebNativeShareAvailable: Boolean,
        @SerializedName("webDisplayMode") var webDisplayMode: String
    )

    data class ConfigInfoData(
        @SerializedName("appInstallData") var appInstallData: String
    )

    data class ClientData(
        @SerializedName("acceptHeader") var acceptHeader: String,
        @SerializedName("browserName") var browserName: String,
        @SerializedName("browserVersion") var browserVersion: String,
        @SerializedName("clientFormFactor") var clientFormFactor: String,
        @SerializedName("clientName") var clientName: String,
        @SerializedName("clientVersion") var clientVersion: String,
        @SerializedName("configInfo") var ConfigInfo: ConfigInfoData,
        @SerializedName("connectionType") var connectionType: String,
        @SerializedName("deviceExperimentId") var deviceExperimentId: String,
        @SerializedName("deviceMake") var deviceMake: String,
        @SerializedName("deviceModel") var deviceModel: String,
        @SerializedName("gl") var gl: String,
        @SerializedName("hl") var hl: String,
        @SerializedName("mainAppWebInfo") var MainAppWebInfo: MainAppWebInfoData,
        @SerializedName("memoryTotalKbytes") var memoryTotalKbytes: String,
        @SerializedName("originalUrl") var originalUrl: String,
        @SerializedName("osName") var osName: String,
        @SerializedName("osVersion") var osVersion: String,
        @SerializedName("platform") var platform: String,
        @SerializedName("remoteHost") var remoteHost: String,
        @SerializedName("screenDensityFloat") var screenDensityFloat: Float,
        @SerializedName("screenHeightPoints") var screenHeightPoints: Int,
        @SerializedName("screenPixelDensity") var screenPixelDensity: Int,
        @SerializedName("screenWidthPoints") var screenWidthPoints: Int,
        @SerializedName("timeZone") var timeZone: String,
        @SerializedName("userAgent") var userAgent: String,
        @SerializedName("userInterfaceTheme") var userInterfaceTheme: String,
        @SerializedName("utcOffsetMinutes") var utcOffsetMinutes: Int,
        @SerializedName("visitorData") var visitorData: String
    )

    data class WebClientInfoData(
        @SerializedName("isDocumentHidden") var isDocumentHidden: Boolean
    )

    data class ContextData(
        @SerializedName("client") var client: ClientData,
        @SerializedName("request") var request: RequestData,
        @SerializedName("user") var User: UserData
    )

    data class YoutubeChatRequestData(
        @SerializedName("context") var Context: ContextData,
        @SerializedName("continuation") var Continuation: String,
        @SerializedName("webClientInfo") var WebClientInfo: WebClientInfoData
    )
}
