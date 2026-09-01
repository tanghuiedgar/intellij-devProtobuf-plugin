package io.kanro.idea.plugin.protobuf.linejump;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.JBColor;
import io.kanro.idea.plugin.protobuf.ui.AmsIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @BelongsPackage: com.tanghui.provider
 * @Author: 唐煇
 * @CreateTime: 2025-02-21-11:13
 * @Description: 类型简单线标记提供程序。
 * @Version: v1.0
 * @param <F> the type parameter
 * @param <T> the type parameter
 */
public abstract class SimpleLineMarkerProvider<F extends PsiElement, T> extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!isTheElement(element)) {
            return;
        }

        Optional<? extends T[]> processResult = apply((F) element);
        if (processResult.isPresent()) {
            T[] arrays = processResult.get();
            NavigationGutterIconBuilder<PsiElement> navigationGutterIconBuilder = NavigationGutterIconBuilder.create(getIcon());
            if (arrays.length > 0) {
                navigationGutterIconBuilder.setTooltipTitle(getTooltip(arrays[0], element));
            }
            // 转换为 List 并排序（假设 T 是 PsiElement 类型，可根据实际类型调整）
            List<PsiElement> list = (List<PsiElement>) Arrays.stream(arrays)
                    .sorted((target1, target2) -> {
                        // 自定义排序逻辑。以下示例按元素文本内容排序。
                        if (target1 instanceof PsiElement && target2 instanceof PsiElement) {
                            String text1 = ModuleUtilCore.findModuleForPsiElement(((PsiElement) target1)).getName();
                            String text2 = ModuleUtilCore.findModuleForPsiElement(((PsiElement) target2)).getName();
                            return text1.compareToIgnoreCase(text2); // 按字母顺序排序，忽略大小写
                        }
                        return 0; // 默认不变
                    })
                    .toList();// 转换为 List

            // 设置导航位置
            navigationGutterIconBuilder.setTargets(list);

            navigationGutterIconBuilder.setPopupTitle("选择目标")
                    .setTargetRenderer(() -> new PsiTargetPresentationRenderer<>() {
                        @Override
                        public @NotNull String getElementText(@NotNull PsiElement element) {
                            // 自定义每个目标的文本
                            if (element instanceof PsiJavaReference) {
                                PsiMethod parentOfType = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
                                PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
                                if (parentOfType != null && psiClass != null && psiClass.getNameIdentifier() != null) {
                                    String name = parentOfType.getName();
                                    String psiClassName = psiClass.getNameIdentifier().getText();
                                    PsiDocComment docComment = psiClass.getDocComment();
                                    PsiDocTag[] tags = new PsiDocTag[0];
                                    if (docComment != null) {
                                        tags = docComment.getTags();
                                    }
                                    String text = "";
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
                                    return psiClassName + "." + name + text ;
                                }
                            }
                            return element.getText(); // 默认文本
                        }

                        @Override
                        public @Nls @Nullable String getContainerText(@NotNull PsiElement element) {
                            // 提供额外的容器信息（例如字段所属的类）
                            if (element instanceof PsiJavaReference) {
                                PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
                                if (psiClass != null) {
                                    Module moduleForPsiElement = ModuleUtilCore.findModuleForPsiElement(psiClass);
                                    if (moduleForPsiElement != null) {
                                        String[] split = moduleForPsiElement.getName().split("\\.");
                                        if (split.length > 1) {
                                            return  "所属模块: " + split[split.length - 2] + "." + split[split.length - 1];
                                        } else {
                                            String module = moduleForPsiElement.getName();
                                            return  "所属模块: " + module;
                                        }
                                    }
                                }
                            }
                            return null;
                        }
                        @Override
                        public @NotNull Icon getIcon(@NotNull PsiElement element) {
                            return AmsIcons.IMPLEMENTED_RPC_DARK;
                        }

                        @Override
                        public @NotNull TargetPresentation getPresentation(@NotNull PsiElement element) {
                            TextAttributes presentableTextbutes = new TextAttributes();
                            presentableTextbutes.setForegroundColor(JBColor.RED);
                            return TargetPresentation.builder(getElementText(element))
                                    .locationText(getContainerText(element))
                                    .presentableTextAttributes(presentableTextbutes)
                                    .icon(getIcon(element))
                                    .presentation();
                        }
                    });
            RelatedItemLineMarkerInfo<PsiElement> lineMarkerInfo = navigationGutterIconBuilder.createLineMarkerInfo(element);
            result.add(lineMarkerInfo);
        }

    }


    /**
     * 该元素是布尔值。
     *
     * @param element the element
     * @return the boolean
     */
    public abstract boolean isTheElement(@NotNull PsiElement element);

    /**
     * 应用可选
     *
     * @param from the from
     * @return the optional
     */
    public abstract Optional<? extends T[]> apply(@NotNull F from);


    /**
     * Gets icon.
     *
     * @return the icon
     */
    @Override
    @NotNull
    public abstract Icon getIcon();

    @NotNull
    public abstract String getTooltip(T array, @NotNull PsiElement target);


    public static String trimLastSymbol(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        // 判断最后一个字符
        char lastChar = str.charAt(str.length() - 1);

        // 如果是字母、数字、汉字，直接返回
        if (Character.isLetterOrDigit(lastChar) || isChinese(lastChar)) {
            return str;
        }

        // 否则去掉最后一个字符
        return str.substring(0, str.length() - 1);
    }

    // 判断是否中文字符
    private static boolean isChinese(char c) {
        return String.valueOf(c).matches("[\\u4e00-\\u9fa5]");
    }

    /**
     * 首字母小写
     * */
    public static String firstCharToLower(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        } else {
            return str.substring(0, 1).toLowerCase() + str.substring(1);
        }
    }
}
