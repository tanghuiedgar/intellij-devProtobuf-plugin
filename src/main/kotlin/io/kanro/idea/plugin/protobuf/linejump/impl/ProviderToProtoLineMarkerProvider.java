package io.kanro.idea.plugin.protobuf.linejump.impl;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import io.kanro.idea.plugin.protobuf.ui.AmsIcons;
import io.kanro.idea.plugin.protobuf.java.JavaNameIndex;
import io.kanro.idea.plugin.protobuf.lang.ProtobufFileType;
import io.kanro.idea.plugin.protobuf.lang.psi.proto.ProtobufElement;
import io.kanro.idea.plugin.protobuf.lang.psi.proto.ProtobufRpcDefinition;
import io.kanro.idea.plugin.protobuf.lang.psi.proto.ProtobufServiceDefinition;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * @BelongsPackage: com.tanghuidev.idea.plugin.protobuf.provider.impl
 * @Author: 唐煇
 * @CreateTime: 2025-04-11-14:32
 * @Description: 实现provider到proto跳转。
 * @Version: v1.0
 */
public class ProviderToProtoLineMarkerProvider extends RelatedItemLineMarkerProvider {

    @Override
    public void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!(element instanceof PsiClass psiClass)) {
            return;
        }
        PsiClass superClass = psiClass.getSuperClass();
        if (superClass != null) {
            DumbService dumbService = DumbService.getInstance(element.getProject());
            // 验证是否索引已完成
            if (dumbService.isDumb()) {
                // 如果索引尚未完成，等待结束后再执行
                dumbService.runWhenSmart(() -> goToTheGRPCService(psiClass, superClass, element, result));
                return;
            }
            // 索引已经完成，直接执行查询
            goToTheGRPCService(psiClass, superClass, element, result);
        }
    }

    private void goToTheGRPCService(PsiClass psiClass, PsiClass superClass, PsiElement element, Collection<? super RelatedItemLineMarkerInfo<?>> result){
        String serviceName = superClass.getQualifiedName();
        StubIndexKey<String, ProtobufElement> indexKey = new JavaNameIndex().getKey();
        // 搜索服务定义
        List<ProtobufElement> elements = StubIndex.getElements(
                indexKey,
                StringUtils.isNotBlank(serviceName) ? serviceName : "",
                element.getProject(),
                GlobalSearchScope.getScopeRestrictedByFileTypes(
                        GlobalSearchScope.allScope(element.getProject()), ProtobufFileType.INSTANCE),
                ProtobufElement.class
        ).stream().toList();
        if (!elements.isEmpty()) {
            for (ProtobufElement elementsElement : elements) {
                if (elementsElement instanceof ProtobufServiceDefinition protobufService) {
                    // 设置导航位置
                    RelatedItemLineMarkerInfo<PsiElement> lineMarkerInfo = NavigationGutterIconBuilder.create(AmsIcons.IMPLEMENTED_SERVICE_DARK).setTarget(protobufService)
                            .setTooltipTitle("跳转grpc服务")
                            .createLineMarkerInfo(Objects.requireNonNull(psiClass.getNameIdentifier()));
                    result.add(lineMarkerInfo);
                    PsiMethod[] methods = psiClass.getMethods();
                    List<ProtobufRpcDefinition> rpcDefinitionList = Objects.requireNonNull(protobufService.getServiceBody()).getRpcDefinitionList();
                    for (PsiMethod method : methods) {
                        // 检查是否重写了父类或接口的方法
                        if (method.findSuperMethods().length > 0) {
                            String name = method.getName();
                            rpcDefinitionList.stream()
                                    .filter(v-> name.equalsIgnoreCase(Objects.requireNonNull(v.getIdentifier()).getText()))
                                    .findFirst().ifPresent(v -> {
                                        RelatedItemLineMarkerInfo<PsiElement> lineMarkerInfoMethod = NavigationGutterIconBuilder.create(AmsIcons.IMPLEMENTED_RPC_DARK)
                                                .setTarget(v)
                                                .setTooltipTitle("跳转grpc方法")
                                                .createLineMarkerInfo(Objects.requireNonNull(method.getNameIdentifier()));
                                        result.add(lineMarkerInfoMethod);
                                    });
                        }
                    }
                }
            }
        }
    }
}
