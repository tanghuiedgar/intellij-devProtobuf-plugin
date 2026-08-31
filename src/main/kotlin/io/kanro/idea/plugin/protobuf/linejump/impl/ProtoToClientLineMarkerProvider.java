package io.kanro.idea.plugin.protobuf.linejump.impl;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiJavaCodeReferenceElementImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.spring.model.utils.SpringCommonUtils;
import com.intellij.ui.JBColor;
import io.kanro.idea.plugin.protobuf.ui.AmsIcons;
import io.kanro.idea.plugin.protobuf.lang.psi.proto.ProtobufServiceDefinition;
import io.kanro.idea.plugin.protobuf.lang.psi.proto.impl.ProtobufPackageStatementImpl;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static io.kanro.idea.plugin.protobuf.linejump.SimpleLineMarkerProvider.trimLastSymbol;
import static io.kanro.idea.plugin.protobuf.ui.AmsIcons.AMS_RESOURCE;

/**
 * @BelongsPackage: com.tanghui.provider
 * @Author: 唐煇
 * @CreateTime: 2025-02-21-11:16
 * @Description: 实现proto到客户端跳转。
 * @Version: v1.0
 */
public class ProtoToClientLineMarkerProvider extends RelatedItemLineMarkerProvider {

    @Override
    public void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!(element instanceof ProtobufServiceDefinition)) {
            return;
        }
        // 服务类
        Project project = element.getProject();
        String serverNameText = Objects.requireNonNull(((ProtobufServiceDefinition) element).getIdentifier()).getText();
        String packageDef = "";
        @NotNull PsiElement[] children = element.getParent().getChildren();
        for (PsiElement child : children) {
            if (child instanceof ProtobufPackageStatementImpl) {
                List<String> list = ((ProtobufPackageStatementImpl) child).getPackageNameList().stream().map(PsiElement::getText).toList();
                 packageDef = String.join(".", list) + ".";
                break;
            }
        }
        AtomicReference<List<PsiElement>> collect = new AtomicReference<>(new ArrayList<>());
        Optional.of(JavaPsiFacade.getInstance(project)
                        .findClasses(packageDef + serverNameText + "Grpc." + serverNameText + "BlockingStub", GlobalSearchScope.allScope(project)))
                .ifPresent(psiClassArrays -> Arrays.asList(psiClassArrays)
                        .forEach(psiClass -> {
                            List<PsiElement> elementList = collect.get();
                            elementList.addAll(findReferences(psiClass));
                            collect.set(elementList);
                        }));
        Optional.of(JavaPsiFacade.getInstance(project)
                        .findClasses(packageDef + serverNameText + "Grpc." + serverNameText + "Stub", GlobalSearchScope.allScope(project)))
                .ifPresent(psiClassArrays -> Arrays.asList(psiClassArrays)
                        .forEach(psiClass -> {
                            List<PsiElement> elementList = collect.get();
                            elementList.addAll(findReferences(psiClass));
                            collect.set(elementList);
                        }));


        Optional<PsiElement[]> processResult = collect.get().isEmpty() ? Optional.empty() : Optional.of(collect.get().stream().distinct().toList().toArray(new PsiElement[0]));

        if (processResult.isPresent()) {
            PsiElement[] arrays = processResult.get();
            NavigationGutterIconBuilder<PsiElement> navigationGutterIconBuilder = NavigationGutterIconBuilder.create(AmsIcons.IMPLEMENTED_SERVICE_DARK);
            if (arrays.length > 0) {
                navigationGutterIconBuilder.setTooltipTitle("跳转grpc客户端");
            }

            navigationGutterIconBuilder.setTargetRenderer(() -> new PsiTargetPresentationRenderer<>() {
                @Override
                public @NotNull String getElementText(@NotNull PsiElement element) {
                    // 自定义每个目标的文本
                    if (element instanceof PsiField psiField) {
                        String name = psiField.getName();
                        if (element.getParent() instanceof PsiClass psiClass && psiClass.getNameIdentifier() != null) {
                            String psiClassName = psiClass.getNameIdentifier().getText();
                            PsiDocComment docComment = psiClass.getDocComment();
                            String text = "";
                            if (docComment != null) {
                                PsiDocTag[] tags = docComment.getTags();
                                for (PsiDocTag tag : tags) {
                                    if (tag.getName().contains("Description")) {  // 注意：不带 @
                                        PsiElement[] dataElements = tag.getDataElements();
                                        StringBuilder description = new StringBuilder();
                                        for (PsiElement dataElement : dataElements) {
                                            if (!(dataElement instanceof PsiWhiteSpace)) {
                                                description.append(dataElement.getText());
                                            }
                                        }
                                        text = " (" + trimLastSymbol(description.toString().trim()) + ") ";
                                    }
                                }
                            }
                            return psiClassName + "." + name + text ;
                        }
                        return name;
                    }
                    return element.getText(); // 默认文本
                }
                @Override
                public @NotNull Icon getIcon(@NotNull PsiElement element) {
                    return AmsIcons.IMPLEMENTED_SERVICE_DARK;
                }
                @Override
                public @NotNull TargetPresentation getPresentation(@NotNull PsiElement element) {
                    TextAttributes presentableTextbutes = new TextAttributes();
                    presentableTextbutes.setForegroundColor(JBColor.RED);
                    Module moduleForPsiElement = ModuleUtilCore.findModuleForPsiElement(element.getParent());
                    return TargetPresentation.builder(getElementText(element))
                            .presentableTextAttributes(presentableTextbutes)
                            .locationText(moduleForPsiElement == null ? "" : moduleForPsiElement.getName(), AMS_RESOURCE)
                            .icon(getIcon(element))
                            .presentation();
                }
            });

            List<PsiElement> list = Arrays.stream(arrays)
                    .sorted((target1, target2) -> {
                        // 自定义排序逻辑。以下示例按元素文本内容排序。
                        if (target1 != null && target2 != null && ModuleUtilCore.findModuleForPsiElement(target1) != null
                        && ModuleUtilCore.findModuleForPsiElement(target2) != null) {
                            String text1 = Objects.requireNonNull(ModuleUtilCore.findModuleForPsiElement(target1)).getName();
                            String text2 = Objects.requireNonNull(ModuleUtilCore.findModuleForPsiElement(target2)).getName();
                            return text1.compareToIgnoreCase(text2); // 按字母顺序排序，忽略大小写
                        }
                        return 0; // 默认不变
                    })
                    .toList();// 转换为 List

            // 设置导航位置
            navigationGutterIconBuilder.setTargets(list);
            RelatedItemLineMarkerInfo<PsiElement> lineMarkerInfo = navigationGutterIconBuilder.createLineMarkerInfo(element);
            result.add(lineMarkerInfo);
        }

    }

    public static List<PsiElement> findReferences(PsiClass psiClass) {
        // 查找类的所有引用
        @NotNull Collection<PsiReference> references = ReferencesSearch.search(psiClass).findAll();
        List<PsiElement> collect = new ArrayList<>();
        for (PsiReference reference : references) {
            // 打印引用的位置
            if (reference instanceof PsiJavaCodeReferenceElementImpl element) {
                PsiElement parent = element.getParent();
                PsiElement javaClazz = getJavaClazz(parent);
                if (javaClazz instanceof PsiField && javaClazz.getParent() instanceof PsiClass) {
                    // 判断类是否是spring管理类
                    if (SpringCommonUtils.isSpringBeanCandidateClass((PsiClass) javaClazz.getParent())) {
                        collect.add(javaClazz);
                    }
                }
            }
        }
        return collect;
    }

    public static PsiElement getJavaClazz(PsiElement parent) {
        if (!(parent instanceof PsiField)) {
            if (!(parent instanceof PsiClass)) {
                PsiElement element = parent.getParent();
                return getJavaClazz(element);
            }
        }
        return parent;
    }

}
