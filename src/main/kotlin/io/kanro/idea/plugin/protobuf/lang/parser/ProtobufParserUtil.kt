package io.kanro.idea.plugin.protobuf.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import io.kanro.idea.plugin.protobuf.lang.psi.proto.token.ProtobufKeywordToken
import io.kanro.idea.plugin.protobuf.lang.psi.proto.token.ProtobufTokens

object ProtobufParserUtil : GeneratedParserUtilBase() {

    @JvmStatic
    fun parseKeyword(builder: PsiBuilder, level: Int): Boolean {
        if (builder.eof()) {
            return false
        }
        val tokenType = builder.tokenType
        if (tokenType is ProtobufKeywordToken) {
            builder.remapCurrentToken(ProtobufTokens.IDENTIFIER_LITERAL)
            builder.advanceLexer()
            return true
        }
        return false
    }
}
