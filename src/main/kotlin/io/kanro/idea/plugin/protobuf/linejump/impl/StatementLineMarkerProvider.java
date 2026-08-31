package io.kanro.idea.plugin.protobuf.linejump.impl;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiJavaCodeReferenceElementImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.spring.model.utils.SpringCommonUtils;
import io.kanro.idea.plugin.protobuf.linejump.SimpleLineMarkerProvider;
import io.kanro.idea.plugin.protobuf.ui.AmsIcons;
import io.kanro.idea.plugin.protobuf.lang.psi.proto.ProtobufElement;
import io.kanro.idea.plugin.protobuf.lang.psi.proto.ProtobufIdentifier;
import io.kanro.idea.plugin.protobuf.lang.psi.proto.ProtobufRpcDefinition;
import io.kanro.idea.plugin.protobuf.lang.psi.proto.ProtobufServiceDefinition;
import io.kanro.idea.plugin.protobuf.lang.psi.proto.impl.ProtobufPackageStatementImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * @BelongsPackage: com.tanghui.provider
 * @Author: 唐煇
 * @CreateTime: 2025-02-21-11:16
 * @Description: 实现proto到java跳转。
 * @Version: v1.0
 */
public class StatementLineMarkerProvider extends SimpleLineMarkerProvider<ProtobufElement, PsiElement> {

    @Override
    public boolean isTheElement(@NotNull PsiElement element) {

        return element instanceof ProtobufElement;
    }

    @Override
    public Optional<? extends PsiElement[]> apply(@NotNull ProtobufElement from) {
        if (from instanceof ProtobufRpcDefinition) {
            ProtobufIdentifier identifier = ((ProtobufRpcDefinition) from).getIdentifier();
            if (identifier == null) {
                return Optional.empty();
            }
            String text = Objects.requireNonNull(identifier).getText();
            // 获取方法名称
            String methodTextName = firstCharToLower(text);
            ProtobufIdentifier protobufIdentifier = ((ProtobufServiceDefinition) from.getParent().getParent()).getIdentifier();
            if (protobufIdentifier == null) {
                return Optional.empty();
            }
            String serverNameText = Objects.requireNonNull(protobufIdentifier).getText();
            String packageDef = "";
            @NotNull PsiElement[] children = from.getParent().getParent().getParent().getChildren();
            for (PsiElement child : children) {
                if (child instanceof ProtobufPackageStatementImpl) {
                    List<String> list = ((ProtobufPackageStatementImpl) child).getPackageNameList().stream().map(PsiElement::getText).toList();
                    packageDef = String.join(".", list) + ".";
                    break;
                }
            }
            Optional<List<PsiElement>> usages = findUsages(from.getProject(), packageDef + serverNameText + "Grpc." + serverNameText + "BlockingStub", methodTextName);
            if (usages.isPresent()) {
                List<PsiElement> elementList = usages.get();
                return elementList.isEmpty() ? Optional.empty() : Optional.of(elementList.toArray(new PsiElement[0]));
            } else {
                Optional<List<PsiElement>> psiElements = findUsages(from.getProject(), packageDef + serverNameText + "Grpc." + serverNameText + "Stub", methodTextName);
                if (psiElements.isPresent()) {
                    List<PsiElement> elementList = psiElements.get();
                    return elementList.isEmpty() ? Optional.empty() : Optional.of(elementList.toArray(new PsiElement[0]));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public @Nullable("null means disabled")
    String getName() {
        return "Statement line marker";
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return AmsIcons.IMPLEMENTED_RPC_DARK;
    }


    @Override
    @NotNull
    public String getTooltip(PsiElement element, @NotNull PsiElement target) {
        String text = null;
        if (element instanceof PsiMethod psiMethod) {
            PsiClass containingClass = psiMethod.getContainingClass();
            if (containingClass != null) {
                text = containingClass.getName() + "#" + psiMethod.getName();
            }
        }
        if (text == null && element instanceof PsiClass psiClass) {
            text = psiClass.getQualifiedName();
        }
        if (text == null && element instanceof PsiField psiField) {
            text = psiField.getName();
        }
        if (text == null && element instanceof PsiReferenceExpression psiReferenceExpression) {
            PsiClass containingClass = findContainingClass(psiReferenceExpression);
            if (containingClass != null) {
                text = containingClass.getName() + "-" +psiReferenceExpression.getText();
            } else {
                text = psiReferenceExpression.getText();
            }
        }
        if (text == null) {
            text = target.getContainingFile().getText();
        }
        return "跳转到 - " + text;
    }

    /**
     * 根据类名和方法名在项目中查找引用。
     *
     * @param project   当前项目
     * @param className 目标类名（全限定名，例如 "com.example.MyClass"）
     * @param methodName 目标方法名（可选，不指定则查找整个类的引用）
     * @return 找到的引用位置列表
     */
    public static Optional<List<PsiElement>> findUsages(Project project, String className, String methodName) {
        // 使用 JavaPsiFacade 查找目标类
        PsiClass psiClass = JavaPsiFacade.getInstance(project)
                .findClass(className, GlobalSearchScope.allScope(project));

        if (psiClass == null) {
            return Optional.empty(); // 未找到类
        }

        // 如果未指定方法名，则查找整个类的引用
        if (methodName == null || methodName.isEmpty()) {
            return Optional.of(findReferences(psiClass));
        }
        // 在类内查找指定的方法
        PsiMethod[] methods = psiClass.findMethodsByName(methodName, true);
        if (methods.length == 0) {
            return Optional.empty(); // 未找到方法
        }
        // 查找方法引用
        List<PsiElement> methodReferences = new ArrayList<>();
        for (PsiMethod method : methods) {
            methodReferences.addAll(findReferences(method));
        }
        return methodReferences.isEmpty() ? Optional.empty() : Optional.of(methodReferences);
    }

    /**
     * 查找 PsiElement 的所有引用
     *
     * @param element 目标 PSI 元素
     * @return 引用的位置列表
     */
    public static List<PsiElement> findReferences(PsiElement element) {
        Collection<PsiReference> references = ReferencesSearch.search(element).findAll();
        List<PsiElement> usageList = new ArrayList<>();
        for (PsiReference reference : references) {
            PsiElement element1 = reference.getElement();
            PsiClass containingClass = findContainingClass(element1);
            if (SpringCommonUtils.isSpringBeanCandidateClass(containingClass)) {
                usageList.add(element1); // 获取引用元素
            }
        }
        return usageList;
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

    /**
     * 判断给定的 PsiElement 是否属于某个 PsiClass。
     *
     * @param element 要检查的元素
     * @return 如果属于 PsiClass，则返回找到的 PsiClass；否则返回 null
     */
    public static PsiClass findContainingClass(PsiElement element) {
        // 遍历 PSI 树向上查找
        while (element != null) {
            if (element instanceof PsiClass) {
                return (PsiClass) element;
            }
            element = element.getContext(); // 获取父级元素
        }
        return null; // 没有找到 PsiClass
    }

}
