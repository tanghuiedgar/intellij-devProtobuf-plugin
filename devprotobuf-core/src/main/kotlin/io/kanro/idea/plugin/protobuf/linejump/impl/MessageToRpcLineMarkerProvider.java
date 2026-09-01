package io.kanro.idea.plugin.protobuf.linejump.impl;


import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.JBColor;
import io.kanro.idea.plugin.protobuf.lang.ProtobufFileType;
import io.kanro.idea.plugin.protobuf.lang.psi.proto.ProtobufIdentifier;
import io.kanro.idea.plugin.protobuf.lang.psi.proto.ProtobufMessageDefinition;
import io.kanro.idea.plugin.protobuf.lang.psi.proto.impl.ProtobufLineCommentImpl;
import io.kanro.idea.plugin.protobuf.lang.psi.proto.impl.ProtobufRpcDefinitionImpl;
import io.kanro.idea.plugin.protobuf.ui.AmsIcons;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.kanro.idea.plugin.protobuf.ui.AmsIcons.PROTO_FILE;

public class MessageToRpcLineMarkerProvider extends RelatedItemLineMarkerProvider {

    @Override
    public void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!(element instanceof ProtobufMessageDefinition messageDefinition)) {
            return;
        }
        ProtobufIdentifier identifier = messageDefinition.getIdentifier();
        if (identifier != null) {
            GlobalSearchScope baseScope = GlobalSearchScope.projectScope(element.getProject());
            // 限定只在 proto 文件里
            GlobalSearchScope protoScope =
                    GlobalSearchScope.getScopeRestrictedByFileTypes(baseScope, ProtobufFileType.INSTANCE);

            Collection<PsiReference> references =
                    ReferencesSearch.search(element, protoScope).findAll();

            // 使用 ReferencesSearch API 查找引用
            List<PsiElement> list = references.stream().map(PsiReference::getElement).toList();
            NavigationGutterIconBuilder<PsiElement> navigationGutterIconBuilder = NavigationGutterIconBuilder.create(AmsIcons.PROCEDUREHTTP_DARK);
            // 设置导航位置
            RelatedItemLineMarkerInfo<PsiElement> lineMarkerInfo = navigationGutterIconBuilder.setTargets(list)
                    .setTooltipTitle("跳转到引用地址")
                    .setTargetRenderer(() -> new PsiTargetPresentationRenderer<>() {
                        @Override
                        public @NotNull String getElementText(@NotNull PsiElement element) {
                            // 自定义每个目标的文本
                            ProtobufRpcDefinitionImpl rpcDefinition = PsiTreeUtil.getParentOfType(element, ProtobufRpcDefinitionImpl.class);
                            if (rpcDefinition != null) {
                                List<ProtobufLineCommentImpl> comments = rpcDefinition.getLineCommentList();
                                String commentText = "";
                                if (CollectionUtils.isNotEmpty(comments)) {
                                    commentText = comments.stream().map(v -> v.getText().substring(2).trim())
                                            .collect(Collectors.joining(","));
                                }
                                /*PsiElement commentForElement = findCommentForElement(rpcDefinition);

                                if (commentForElement != null) {
                                    commentText = commentForElement.getText().substring(2).trim();
                                }*/
                                return Objects.requireNonNull(rpcDefinition.getIdentifier()).getText() + " (" + commentText + ")";
                            }
                            return element.getText(); // 默认文本
                        }
                        @Override
                        public @NotNull Icon getIcon(@NotNull PsiElement element) {
                            return AmsIcons.PROCEDUREHTTP_DARK;
                        }
                        @Override
                        public @NotNull TargetPresentation getPresentation(@NotNull PsiElement element) {
                            TextAttributes presentableTextbutes = new TextAttributes();
                            presentableTextbutes.setForegroundColor(JBColor.RED);
                            PsiFile psiFile = element.getContainingFile();
                            return TargetPresentation.builder(getElementText(element))
                                    .presentableTextAttributes(presentableTextbutes)
                                    .locationText(psiFile.getName(), PROTO_FILE)
                                    .icon(getIcon(element))
                                    .presentation();
                        }
                    })
                    .createLineMarkerInfo(element);
            result.add(lineMarkerInfo);
        }
    }

}
