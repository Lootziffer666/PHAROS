package com.flow.pharos.util

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream

class PdfTextExtractor {
    fun extractText(inputStream: InputStream, maxChars: Int = TextExtractor.MAX_CHARS): String? {
        return try {
            PDDocument.load(inputStream).use { document ->
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                if (text.isNullOrBlank()) return null
                if (text.length > maxChars) {
                    text.substring(0, maxChars) + "\n[... truncated at $maxChars characters ...]"
                } else {
                    text
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
