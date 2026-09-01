package io.kanro.idea.plugin.protobuf.lang.psi.proto.element

import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import com.intellij.psi.util.parentOfType
import io.kanro.idea.plugin.protobuf.ProtobufIcons
import io.kanro.idea.plugin.protobuf.lang.psi.findChild
import io.kanro.idea.plugin.protobuf.lang.psi.findChildren
import io.kanro.idea.plugin.protobuf.lang.psi.findLastChild
import io.kanro.idea.plugin.protobuf.lang.psi.proto.ProtobufElement
import io.kanro.idea.plugin.protobuf.lang.psi.proto.ProtobufRpcIO
import io.kanro.idea.plugin.protobuf.lang.psi.proto.ProtobufServiceDefinition
import io.kanro.idea.plugin.protobuf.lang.psi.proto.impl.ProtobufLineCommentImpl
import io.kanro.idea.plugin.protobuf.lang.psi.proto.stream
import io.kanro.idea.plugin.protobuf.lang.psi.proto.structure.ProtobufDefinition
import javax.swing.Icon

interface ProtobufRpcDefinition : ProtobufDefinition {
    override fun owner(): ProtobufServiceDefinition? {
        return parentOfType()
    }

    override fun type(): String {
        return "rpc"
    }

    override fun getIcon(unused: Boolean): Icon? {
        val parameters = findChildren<ProtobufRpcIO>()
        if (parameters.size != 2) return ProtobufIcons.RPC_METHOD
        val inputStream = parameters[0].stream()
        val outputStream = parameters[1].stream()

        return when {
            inputStream && outputStream -> ProtobufIcons.RPC_METHOD_BISTREAM
            outputStream -> ProtobufIcons.RPC_METHOD_SERVER_STREAM
            inputStream -> ProtobufIcons.RPC_METHOD_CLIENT_STREAM
            else -> ProtobufIcons.RPC_METHOD
        }
    }

    fun input(): ProtobufRpcIO? {
        return findChild()
    }

    fun output(): ProtobufRpcIO? {
        return findLastChild()
    }

    fun getLineCommentList(): List<ProtobufLineCommentImpl> {
        val list = mutableListOf<ProtobufLineCommentImpl>()
        var temp = this.prevSibling
        while (temp != null && (temp is ProtobufElement || temp is PsiWhiteSpaceImpl)){
            if (temp is PsiWhiteSpaceImpl) {
                temp = temp.prevSibling
                continue
            }
            if (temp is ProtobufLineCommentImpl) {
                list.add(temp)
                temp = temp.prevSibling
            } else {
                break
            }
        }
        return list
    }

    override fun tailText(): String? {
        val parameters = findChildren<ProtobufRpcIO>()
        if (parameters.size != 2) return "()"
        var input = parameters[0].typeName.leaf().text ?: return "()"
        var output = parameters[1].typeName.leaf().text ?: return "()"
        if (parameters[0].stream()) {
            input = "stream $input"
        }
        if (parameters[1].stream()) {
            output = "stream $output"
        }
        return "($input): $output"
    }
}
