package com.kronos.router;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.kronos.router.exception.ModuleNameNotFoundException;
import com.kronos.router.utils.Constants;
import com.kronos.router.utils.EmptyUtils;
import com.kronos.router.utils.Logger;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;

@AutoService(Processor.class)
@SuppressWarnings("NullAway")
public class RouterProcessor extends AbstractProcessor {
    private Filer filer;
    private Logger logger;
    private String moduleName;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        Messager messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
        logger = new Logger(messager);
        logger.info("start processor");
        Map<String, String> options = processingEnv.getOptions();
        if (!EmptyUtils.isEmpty(options)) {
            moduleName = options.get(Constants.MODULE_NAME);
        }
    }


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> ret = new HashSet<>();
        ret.add(BindRouter.class.getCanonicalName());
        return ret;
    }

    @Override
    public Set<String> getSupportedOptions() {
        return ImmutableSet.of("com.kronos.router.BindRouter");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if(EmptyUtils.isEmpty(moduleName)){
            throw new ModuleNameNotFoundException("必须要在对应的module下面配置moduleName");
        }else{
            initRouter(moduleName, roundEnv);
        }
        return true;
    }

    private void initRouter(String name, RoundEnvironment roundEnv) {
        MethodSpec.Builder initMethod = MethodSpec.methodBuilder("init")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC);
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(BindRouter.class);
        logger.warning("size：" + elements.size() + "; moduleName = " + moduleName);
        //一、收集信息
        int count = 0;
        for (Element element : elements) {
            //检查element类型
            //field type
            BindRouter router = element.getAnnotation(BindRouter.class);
            ClassName className;
            if (element.getKind() == ElementKind.CLASS) {
                className = ClassName.get((TypeElement) element);
            } else if (element.getKind() == ElementKind.METHOD) {
                className = ClassName.get((TypeElement) element.getEnclosingElement());
            } else {
                throw new IllegalArgumentException("unknow type");
            }
            //class type
            String[] id = router.urls();
            for (String format : id) {
                int weight = router.weight();
                if (router.isRunnable()) {
                    String callbackName = "callBack" + count;
                    initMethod.addStatement(className + " " + callbackName + "=new " + className + "()");
                    initMethod.addStatement("com.kronos.router.Router.map($S, " + callbackName + ")", format);
                    count++;
                    continue;
                }
                if (weight > 0) {
                    String bundleName = "bundle" + count;
                    initMethod.addStatement("android.os.Bundle " + bundleName + "=new android.os.Bundle();");
                    String optionsName = "options" + count;
                    initMethod.addStatement("com.kronos.router.model.RouterOptions " + optionsName + "=new com.kronos.router.model.RouterOptions("
                            + bundleName + ")");
                    initMethod.addStatement(optionsName + ".setWeight(" + weight + ")");
                    initMethod.addStatement("com.kronos.router.Router.map($S,$T.class," + optionsName + ")",
                            format, className);
                } else {
                    initMethod.addStatement("com.kronos.router.Router.map($S,$T.class)", format, className);
                }
                count++;
            }
        }
        String moduleName = "RouterInit_" + name;
        TypeSpec routerMapping = TypeSpec.classBuilder(moduleName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(initMethod.build())
                .build();
        try {
            JavaFile.builder("com.kronos.router.init", routerMapping)
                    .build()
                    .writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
