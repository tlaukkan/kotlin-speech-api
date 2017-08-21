package org.bubblecloud.voice

import com.google.cloud.language.v1beta2.*

class Analyzer {

    fun analyse(text: String): List<Token> {
        val language = LanguageServiceClient.create()
        val doc = Document.newBuilder()
                .setContent(text).setType(Document.Type.PLAIN_TEXT).build()
        val request = AnalyzeSyntaxRequest.newBuilder()
                .setDocument(doc)
                .setEncodingType(EncodingType.UTF16).build()
        val response = language.analyzeSyntax(request)
        return response.tokensList
    }

}