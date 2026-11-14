package com.dark.darknama.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class FontSettings(
    // Keep the original property so existing font_settings.json selections migrate intact.
    val fontType: FontType = FontType.DEFAULT,
    val englishFontType: EnglishFontType = EnglishFontType.DEFAULT
) {
    companion object {
        val DEFAULT = FontSettings()
    }

    /** Choose the saved family for the app language, never by the text of a row. */
    fun forLanguage(language: String): AppFont =
        if (language.trim().substringBefore('-').substringBefore('_').equals("fa", ignoreCase = true))
            AppFont.Persian(fontType)
        else AppFont.English(englishFontType)
}

sealed interface AppFont {
    data class Persian(val type: FontType) : AppFont
    data class English(val type: EnglishFontType) : AppFont
}

@Serializable(with = FontTypeSerializer::class)
enum class FontType {
    DEFAULT, VAZIRMATN;

    companion object {
        /** Retired or unknown saved Persian choices safely become DEFAULT. */
        fun fromStoredValue(value: String?): FontType =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: DEFAULT
    }
}

/** Decode unknown persisted font names as DEFAULT instead of discarding all settings. */
object FontTypeSerializer : KSerializer<FontType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FontType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: FontType) = encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder): FontType =
        FontType.fromStoredValue(decoder.decodeString())
}

@Serializable(with = EnglishFontTypeSerializer::class)
enum class EnglishFontType {
    DEFAULT, INTER, POPPINS, MONTSERRAT, NUNITO, LATO;

    companion object {
        fun fromStoredValue(value: String?): EnglishFontType =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: DEFAULT
    }
}

/** Unknown English names must not discard a valid persisted Persian preference. */
object EnglishFontTypeSerializer : KSerializer<EnglishFontType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("EnglishFontType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: EnglishFontType) = encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder): EnglishFontType =
        EnglishFontType.fromStoredValue(decoder.decodeString())
}