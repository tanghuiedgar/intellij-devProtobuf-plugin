package io.kanro.idea.plugin.protobuf.string.case

object LowercaseFormatter : BaseCaseFormatter() {
    override fun formatWord(
        index: Int,
        word: CharSequence,
    ): CharSequence {
        return buildString {
            if (index == 0) {
                return word.toString().lowercase()
            } else {
                append(word)
            }
        }
    }
}
