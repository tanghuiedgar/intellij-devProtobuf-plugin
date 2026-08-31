package io.kanro.idea.plugin.protobuf.linejump.impl;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.Query;
import io.kanro.idea.plugin.protobuf.ui.AmsIcons;
import io.kanro.idea.plugin.protobuf.java.JavaNameIndex;
import io.kanro.idea.plugin.protobuf.lang.ProtobufFileType;
import io.kanro.idea.plugin.protobuf.lang.psi.proto.ProtobufElement;
import io.kanro.idea.plugin.protobuf.lang.psi.proto.ProtobufRpcDefinition;
import io.kanro.idea.plugin.protobuf.lang.psi.proto.ProtobufServiceBody;
import io.kanro.idea.plugin.protobuf.lang.psi.proto.ProtobufServiceDefinition;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static io.kanro.idea.plugin.protobuf.linejump.SimpleLineMarkerProvider.firstCharToLower;


/**
 * @BelongsPackage: com.tanghui.provider
 * @Author: 唐煇
 * @CreateTime: 2025-02-21-11:16
 * @Description: 实现java到proto跳转。
 * @Version: v1.0
 */
public class JavaToProtoLineMarkerProvider extends RelatedItemLineMarkerProvider {

    private static final Logger LOG = LoggerFactory.getLogger(JavaToProtoLineMarkerProvider.class);

    @Override
    public void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!(element instanceof PsiClass)) {
            return;
        }
        Map<PsiField, List<PsiElement>> fieldUsages = findFieldUsages((PsiClass) element);
        if (!fieldUsages.isEmpty()) {
            fieldUsages.forEach((psiField, psiElements) -> {
                PsiType fieldType = psiField.getType(); // 获取字段类型
                String typeCanonicalText = fieldType.getCanonicalText(); // 获取类型的全限定名
                List<ProtobufServiceDefinition> protobufServices = findProtobufServices(element.getProject(), typeCanonicalText);
                if (protobufServices.isEmpty()) {
                    return;
                }
                ProtobufServiceBody serviceBody = protobufServices.getFirst().getServiceBody();
                if (serviceBody == null || serviceBody.getRpcDefinitionList().isEmpty()) {
                    return;
                }
                List<ProtobufRpcDefinition> rpcDefinitionList = serviceBody.getRpcDefinitionList();
                ProtobufServiceDefinition[] array = protobufServices.toArray(new ProtobufServiceDefinition[0]);
                PsiField[] fields = ((PsiClass) element).getFields();
                List<PsiField> list = Arrays.asList(fields);
                if (list.contains(psiField)) {
                    result.add(NavigationGutterIconBuilder.create(AmsIcons.IMPLEMENTED_SERVICE)
                            .setTargets(array)
                            .setTooltipTitle("跳转grpc服务")
                            .createLineMarkerInfo(Objects.requireNonNull(psiField.getNameIdentifier())));
                }
                if (psiElements == null || psiElements.isEmpty()) {
                    return;
                }
                for (PsiElement psiElement : psiElements) {
                    if (psiElement.getParent() != null) {
                        PsiElement finalPsiElement = psiElement;
                        while (true) {
                            PsiElement parent = finalPsiElement.getParent();
                            if (!(parent instanceof PsiReferenceExpressionImpl)) {
                                finalPsiElement = parent;
                                continue;
                            }
                            PsiType type = ((PsiReferenceExpressionImpl) parent).getType();
                            if (type == null) {
                                break;
                            }
                            if (type instanceof PsiClassType classType) {
                                boolean inheritor = InheritanceUtil.isInheritor(classType.resolve(), "io.grpc.stub.AbstractStub");
                                if (!inheritor) {
                                    break;
                                } else {
                                    finalPsiElement = parent;
                                }
                            } else {
                                break;
                            }
                        }

                        @NotNull PsiElement[] children = finalPsiElement.getParent().getChildren();
                        if (children.length > 0) {
                            PsiElement child = children[children.length - 1];
                            rpcDefinitionList.stream().filter(v -> v.getIdentifier() != null && firstCharToLower(child.getText()).equals(firstCharToLower(v.getIdentifier().getText())))
                                    .findFirst().ifPresent(v -> {
                                        try {
                                            NavigationGutterIconBuilder<PsiElement> navigationGutterIconBuilder = NavigationGutterIconBuilder.create(AmsIcons.IMPLEMENTED_RPC);
                                            // 设置导航位置
                                            RelatedItemLineMarkerInfo<PsiElement> lineMarkerInfo = navigationGutterIconBuilder.setTarget(v)
                                                    .setTooltipTitle("跳转grpc方法 -" + child.getText())
                                                    .createLineMarkerInfo(psiElement);
                                            result.add(lineMarkerInfo);
                                        } catch (Exception ignored) {
                                        }
                                    });
                        }
                    }

                }
            });
        }
    }

    public static Map<PsiField, List<PsiElement>> findFieldUsages(PsiClass psiClass) {
        Map<PsiField, List<PsiElement>> fieldUsages = new HashMap<>();
        // 获取目标类的所有成员变量
        PsiField[] fields = psiClass.getAllFields();
        for (PsiField field : fields) {
            List<PsiElement> usages = new ArrayList<>();
            PsiModifierList modifierList = field.getModifierList();
            if (modifierList != null) {
                PsiAnnotation annotation = modifierList.findAnnotation("com.apex.ams.annotation.AmsBlockingStub");
                if (annotation == null) {
                    if (modifierList.findAnnotation("com.apex.ams.annotation.AmsStub") == null) {
                        continue;
                    }
                }
            } else {
                continue;
            }
            // 查找字段的所有引用
            Query<PsiReference> search = ReferencesSearch.search(field, GlobalSearchScope.fileScope(psiClass.getContainingFile()));
            for (PsiReference reference : search) {
                PsiElement element = reference.getElement();
                usages.add(element);
            }
            fieldUsages.put(field, usages);
        }
        return fieldUsages; // 返回所有 PsiElement 列表
    }


    public static List<ProtobufServiceDefinition> findProtobufServices(Project project, String serviceName) {
        List<ProtobufServiceDefinition> serviceDefinitions = new ArrayList<>();
        StubIndexKey<String, ProtobufElement> indexKey = new JavaNameIndex().getKey();
        // 检查是否处于 Dumb 模式
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(() -> {
                // 在索引加载完成后递归调用方法
                List<ProtobufServiceDefinition> definitions = findProtobufServices(project, serviceName);
                serviceDefinitions.addAll(definitions);
            });
        } else {
            try {
                // 搜索服务定义
                List<ProtobufElement> elements = StubIndex.getElements(
                        indexKey,
                        serviceName,
                        project,
                        GlobalSearchScope.getScopeRestrictedByFileTypes(
                                GlobalSearchScope.allScope(project), ProtobufFileType.INSTANCE),
                        ProtobufElement.class
                ).stream().toList();

                // 筛选出服务定义
                for (PsiElement element : elements) {
                    if (element instanceof ProtobufServiceDefinition) {
                        serviceDefinitions.add((ProtobufServiceDefinition) element);
                    }
                }
            } catch (Exception e) {
                LOG.error("Error while retrieving Protobuf services: {}", e.getMessage(), e);
            }
        }
        return serviceDefinitions;
    }

}
