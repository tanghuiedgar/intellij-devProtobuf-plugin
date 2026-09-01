package io.kanro.idea.plugin.protobuf.lang.psi.proto.feature

import com.intellij.openapi.extensions.ExtensionPointName
import io.kanro.idea.plugin.protobuf.lang.psi.proto.ProtobufFile

interface ProtobufStubExternalProvider {
    companion object {
        var extensionPoint: ExtensionPointName<ProtobufStubExternalProvider> =
            ExtensionPointName.create("intellij.devProtobuf.plugin.stubExternalProvider")
    }

    fun mergeExternalData(
        element: ProtobufStubSupport<*, *>,
        external: MutableMap<String, String>,
    )

    fun mergeExternalData(
        file: ProtobufFile,
        external: MutableMap<String, String>,
    )
}
