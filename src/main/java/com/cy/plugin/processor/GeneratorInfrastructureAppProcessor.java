package com.cy.plugin.processor;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.plugin.annotations.GeneratorInfrastructureApplication;
import com.cy.plugin.annotations.GeneratorQueryByThisField;
import com.cy.plugin.annotations.GeneratorRemoveByThisField;
import com.cy.plugin.annotations.GeneratorUpdateByThisField;
import com.cy.plugin.code.error.AdviceErrorCode;
import com.cy.plugin.constant.ResultType;
import com.cy.plugin.exception.BusinessException;
import com.cy.plugin.id.IdGenerator;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Service;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @classDesc: 生成基础设施层应用处理器
 * @author: cyjer
 * @date: 2023/3/6 12:50
 */
@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("com.cy.plugin.annotations.GeneratorInfrastructureApplication")
public class GeneratorInfrastructureAppProcessor extends AbstractProcessor {
    private Messager messager;
    private Filer filer;
    private static final Pattern HUMP_PATTERN = Pattern.compile("[A-Z]");


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        // 通过init()方法，完成初始化工作
        this.messager = processingEnv.getMessager();
        this.filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            String annotationName = annotation.getSimpleName().toString();
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element.getKind() != ElementKind.CLASS) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "@" + annotationName + "must be used for a class");
                }
                GeneratorInfrastructureApplication application = element.getAnnotation(GeneratorInfrastructureApplication.class);
                // 获取被说明类的包名和类名，为创建对应的类做准备
                TypeElement classElement = (TypeElement) element;
                //构造接口描述
                TypeSpec typeSpec = buildMapperInterface(application, classElement);
                // 生成仓储层mapper
                JavaFile javaFile = JavaFile.builder(application.generatorJavaPackageLocation() + ".mapper", typeSpec).build();
                try {
                    javaFile.writeTo(filer);
                    //生成仓储层mapper xml
                    buildMapperXml(application, classElement);
                } catch (Exception e) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate mapper file for class ", element);
                }

                //生成仓储层应用顶层接口及实现
                TypeSpec service = buildApplicationInterface(application, classElement);
                TypeSpec serviceImpl = buildApplicationInterfaceImpl(application, classElement);
                //写入文件
                JavaFile serviceImplFile = JavaFile.builder(application.generatorJavaPackageLocation() + ".application.impl", serviceImpl).build();
                JavaFile serviceFile = JavaFile.builder(application.generatorJavaPackageLocation() + ".application", service).build();
                try {
                    serviceImplFile.writeTo(filer);
                    serviceFile.writeTo(filer);
                } catch (Exception e) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate app file for class ", element);
                }

            }
        }
        return roundEnv.processingOver();
    }

    private TypeSpec buildMapperInterface(GeneratorInfrastructureApplication application, TypeElement classElement) {
        String simpleClassName = classElement.getSimpleName().toString();
        String entityParam = firstCharToLowerCase(simpleClassName);
        TypeMirror typeMirror = classElement.asType();
        ClassName className = ClassName.get(application.generatorJavaPackageLocation() + ".mapper", simpleClassName + "InfrastructureMapper");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return TypeSpec.interfaceBuilder(className)
                .addJavadoc("@author $L\n", application.author())
                .addJavadoc("@classDesc 实体$L基础设施层Mapper \n", simpleClassName)
                .addJavadoc("@date $L", format.format(new Date()))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Mapper.class)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(BaseMapper.class), TypeName.get(typeMirror)))
                .addMethod(MethodSpec.methodBuilder("insertBatch" + simpleClassName)
                        .addJavadoc("基础设施层Mapper已生成批量插入实体$L方法:$L\n\n", simpleClassName, "insertBatch" + simpleClassName)
                        .addJavadoc("@param $L 仓储层实体列表$L\n", entityParam, entityParam)
                        .addJavadoc("@return 是否保存成功")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(typeMirror)), entityParam)
                        .returns(TypeName.BOOLEAN)
                        .build())
                .build();
    }

    private void buildMapperXml(GeneratorInfrastructureApplication application, TypeElement classElement) throws IOException {
        //生成xml文件
        String simpleClassName = classElement.getSimpleName().toString();

        FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, application.generatorXmlPackageLocation(), simpleClassName + "Mapper.xml");
        Writer writer = file.openWriter();

        writer.write(String.format("<!-- %s 仓储层 mapper xml 已生成 -->\n", simpleClassName));
        writer.write("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n");
        writer.write(String.format("<mapper namespace=\"%s.%s\">\n", application.generatorJavaPackageLocation() + ".mapper", simpleClassName + "InfrastructureMapper"));
        writer.write(String.format("\t<resultMap id=\"BaseResultMap\" type=\"%s\">\n\t", classElement.getQualifiedName()));
        //遍历查找类字段
        List<? extends Element> classFields = getClassFields(classElement);

        List<String> databaseFields = new ArrayList<>();
        List<String> entityFields = new ArrayList<>();

        for (Element ele : classFields) {
            String entityField = ele.getSimpleName().toString();
            String snakeCaseDataBaseField = convertToSnakeCase(entityField);
            //剔除不存在的字段
            TableField tableField = ele.getAnnotation(TableField.class);
            if (null != tableField) {
                if (tableField.exist()) {
                    databaseFields.add(snakeCaseDataBaseField);
                }
            } else {
                databaseFields.add(snakeCaseDataBaseField);
            }
            entityFields.add(entityField);
            TableId tableId = ele.getAnnotation(TableId.class);
            if (null != tableId) {
                //判断是否为主键
                writer.write(String.format("\t<id column=\"%s\" property=\"%s\"/>\n\t", snakeCaseDataBaseField, entityField));
            } else {
                writer.write(String.format("\t<result column=\"%s\" property=\"%s\"/>\n\t", snakeCaseDataBaseField, entityField));
            }
        }
        String fieldsToDatabaseFields = fieldsToDatabaseFields(databaseFields);
        writer.write("</resultMap>\n");
        writer.write("\t<sql id=\"Base_Column_List\">\n\t");
        writer.write(String.format("\t%s\n", fieldsToDatabaseFields));
        writer.write("\t</sql>\n");
        writer.write(String.format("\t<insert id=\"insertBatch%s\">\n\t", simpleClassName));
        writer.write(String.format("\tinsert into %s (%s) values\n\t", getTableName(classElement), fieldsToDatabaseFields));
        writer.write("\t<foreach collection=\"list\" item=\"item\" separator=\",\">\n\t");
        writer.write(String.format("\t\t%s\n\t", fieldsToInsertFields(entityFields)));
        writer.write("\t</foreach>\n");
        writer.write("\t</insert>\n");
        writer.write("</mapper>");
        writer.close();
    }

    private TypeSpec buildApplicationInterface(GeneratorInfrastructureApplication application, TypeElement classElement) {
        String simpleClassName = classElement.getSimpleName().toString();
        TypeMirror classTypeMirror = classElement.asType();
        ClassName className = ClassName.get(application.generatorJavaPackageLocation() + ".application", simpleClassName + "Application");
        String entityParam = firstCharToLowerCase(simpleClassName);
        //获取主键元素
        Element idEle = getId(getClassFields(classElement));
        //获取主键类型
        TypeName idType = null == idEle ? TypeName.LONG : TypeName.get(idEle.asType());

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TypeSpec.Builder classBuilder = TypeSpec.interfaceBuilder(className)
                .addJavadoc("@author $L\n", application.author())
                .addJavadoc("@classDesc 实体$L基础设施层应用顶层接口\n", simpleClassName)
                .addJavadoc("@date $L", format.format(new Date()))
                .addModifiers(Modifier.PUBLIC)
                .addMethod(MethodSpec.methodBuilder("saveOrUpdate" + simpleClassName)
                        .addJavadoc("基础设施层应用已生成保存或更新(insert or update)实体$L方法:$L\n\n", simpleClassName, "saveOrUpdate" + simpleClassName)
                        .addJavadoc("@param $L 仓储层实体$L\n", entityParam, entityParam)
                        .addJavadoc("@return {@link $L} 成功保存的仓储层实体$L", simpleClassName, entityParam)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(TypeName.get(classTypeMirror), entityParam)
                        .returns(TypeName.get(classTypeMirror))
                        .build())
                .addMethod(MethodSpec.methodBuilder("saveBatch" + simpleClassName)
                        .addJavadoc("基础设施层应用已生成批量保存实体$L方法:$L\n\n", simpleClassName, "saveBatch" + simpleClassName)
                        .addJavadoc("@param $L 仓储层实体列表$L\n", entityParam, entityParam)
                        .addJavadoc("@return {@link $L} 成功保存的仓储层实体列表$L", simpleClassName, entityParam)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(classTypeMirror)), entityParam)
                        .returns(ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(classTypeMirror)))
                        .build())
                .addMethod(MethodSpec.methodBuilder("select" + simpleClassName + "ByIds")
                        .addJavadoc("基础设施层应用已生成批量根据id查询实体$L方法:$L\n\n", simpleClassName, "select" + simpleClassName + "ByIds")
                        .addJavadoc("@param $L 仓储层实体主键id列表$L\n", entityParam, entityParam)
                        .addJavadoc("@return {@link $L} 查询到的仓储层实体$L列表", simpleClassName, entityParam)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), idType), "ids")
                        .returns(ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(classTypeMirror)))
                        .build())
                .addMethod(MethodSpec.methodBuilder("selectAll" + simpleClassName)
                        .addJavadoc("基础设施层应用已生成查询所有实体$L列表方法:$L\n\n", simpleClassName, "selectAll" + simpleClassName)
                        .addJavadoc("@return {@link $L} 查询到的仓储层实体$L列表", simpleClassName, entityParam)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(classTypeMirror)))
                        .build())
                .addMethod(MethodSpec.methodBuilder("selectList" + simpleClassName)
                        .addJavadoc("基础设施层应用已生成根据条件查询实体$L列表方法:$L\n\n", simpleClassName, "selectList" + simpleClassName)
                        .addJavadoc("@param $L 仓储层实体\n", entityParam)
                        .addJavadoc("@return {@link $L} 查询到的仓储层实体$L列表", simpleClassName, entityParam)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(TypeName.get(classTypeMirror), entityParam)
                        .returns(ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(classTypeMirror)))
                        .build())
                .addMethod(MethodSpec.methodBuilder("count" + simpleClassName)
                        .addJavadoc("基础设施层应用已生成查询实体$L数量方法:$L\n\n", simpleClassName, "count" + simpleClassName)
                        .addJavadoc("@return {@link $L} 查询到的仓储层实体$L数量", simpleClassName, entityParam)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(TypeName.INT.box())
                        .build())
                .addMethod(MethodSpec.methodBuilder("count" + simpleClassName)
                        .addJavadoc("基础设施层应用已生成根据条件查询实体$L数量方法:$L\n\n", simpleClassName, "count" + simpleClassName)
                        .addJavadoc("@param $L 仓储层实体\n", entityParam)
                        .addJavadoc("@return {@link $L} 查询到的仓储层实体$L数量", simpleClassName, entityParam)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(TypeName.get(classTypeMirror), entityParam)
                        .returns(TypeName.INT.box())
                        .build())
                .addMethod(MethodSpec.methodBuilder("pageSelect" + simpleClassName)
                        .addJavadoc("基础设施层应用已生成分页查询实体$L方法:$L\n\n", simpleClassName, "pageSelect" + simpleClassName)
                        .addJavadoc("@param $L 仓储层实体\n", entityParam)
                        .addJavadoc("@param pageNo 当前页\n")
                        .addJavadoc("@param pageSize 每页条数\n")
                        .addJavadoc("@return {@link $L} {@link $T} 查询到的仓储层实体$L分页结果", simpleClassName, Page.class, entityParam)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(TypeName.get(classTypeMirror), entityParam)
                        .addParameter(ParameterSpec.builder(TypeVariableName.get(Integer.class), "pageNo").build())
                        .addParameter(ParameterSpec.builder(TypeVariableName.get(Integer.class), "pageSize").build())
                        .returns(ParameterizedTypeName.get(ClassName.get(Page.class), TypeName.get(classTypeMirror)))
                        .build())
                .addMethod(MethodSpec.methodBuilder("remove" + simpleClassName + "ByIds")
                        .addJavadoc("基础设施层应用已生成批量根据id删除实体$L方法:$L\n\n", simpleClassName, "remove" + simpleClassName + "ByIds")
                        .addJavadoc("@param ids 仓储层实体$L主键列表\n", simpleClassName)
                        .addJavadoc("@return 是否删除成功")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(Long.class)), "ids")
                        .returns(TypeName.get(Boolean.class))
                        .build())
                .addMethod(MethodSpec.methodBuilder("remove" + simpleClassName + "ById")
                        .addJavadoc("基础设施层应用已生成根据id删除实体$L方法:$L\n\n", simpleClassName, "remove" + simpleClassName + "ById")
                        .addJavadoc("@param ids 仓储层实体$L主键列表\n", simpleClassName)
                        .addJavadoc("@return 是否删除成功")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(TypeName.get(Long.class), "id")
                        .returns(TypeName.get(Boolean.class))
                        .build())
                .addMethod(MethodSpec.methodBuilder("remove" + simpleClassName)
                        .addJavadoc("基础设施层应用已生成根据条件删除实体$L方法:$L\n\n", simpleClassName, "remove" + simpleClassName)
                        .addJavadoc("@param $L 仓储层实体$L\n", entityParam, simpleClassName)
                        .addJavadoc("@return 是否删除成功")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(TypeName.get(classTypeMirror), entityParam)
                        .returns(TypeName.BOOLEAN.box())
                        .build());
        //判断字段注解是否存在根据不同注解生成不同方法
        generatorQueryByFieldInterfaceMethod(classElement, classBuilder, classTypeMirror, entityParam, simpleClassName);
        generatorUpdateByFieldInterfaceMethod(classElement, classBuilder, classTypeMirror, entityParam, simpleClassName);
        generatorRemoveByFieldInterfaceMethod(classElement, classBuilder, classTypeMirror, entityParam, simpleClassName);
        return classBuilder.build();
    }

    private TypeSpec buildApplicationInterfaceImpl(GeneratorInfrastructureApplication application, TypeElement classElement) {
        String simpleClassName = classElement.getSimpleName().toString();
        TypeMirror classTypeMirror = classElement.asType();
        ClassName className = ClassName.get(application.generatorJavaPackageLocation() + ".application", simpleClassName + "ApplicationImpl");
        String entityParam = firstCharToLowerCase(simpleClassName);
        //获取主键元素
        Element idEle = getId(getClassFields(classElement));
        //获取主键类型
        TypeName idType = null == idEle ? TypeName.LONG : TypeName.get(idEle.asType());
        //转换为set字符
        String id = firstCharToUpperCase(idEle == null ? "id" : idEle.getSimpleName().toString());
        CodeBlock queryParamCodeBlock = generatorQueryCodeBlock(getDataBaseFieldsByClass(classElement), classTypeMirror, entityParam, true, false, false);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TypeName mapperTypeVariableName = TypeVariableName.get(application.generatorJavaPackageLocation() + ".mapper." + simpleClassName + "InfrastructureMapper");
        TypeName applicationTypeVariableName = TypeVariableName.get(application.generatorJavaPackageLocation() + ".application." + simpleClassName + "Application");
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addJavadoc("@author $L\n", application.author())
                .addJavadoc("@classDesc 实体$L基础设施层应用顶层接口实现\n", simpleClassName)
                .addJavadoc("@date $L", format.format(new Date()))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Service.class)
                .superclass(ParameterizedTypeName.get(ClassName.get(ServiceImpl.class), mapperTypeVariableName, TypeName.get(classTypeMirror)))
                .addSuperinterface(applicationTypeVariableName)
                .addMethod(MethodSpec.methodBuilder("saveOrUpdate" + simpleClassName)
                        .addJavadoc("基础设施层应用已生成保存或更新(insert or update)实体$L方法:$L\n\n", simpleClassName, "saveOrUpdate" + simpleClassName)
                        .addJavadoc("@param $L 仓储层实体$L\n", entityParam, entityParam)
                        .addJavadoc("@return {@link $L} 成功保存的仓储层实体$L", simpleClassName, entityParam)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(TypeName.get(classTypeMirror), entityParam)
                        .addStatement(CodeBlock.builder().indent()
                                .add("boolean save;\n").unindent()
                                .add("if (null != $L.get$L()) {\n", entityParam, id).indent()
                                .add("save = this.updateById($L);\n", entityParam).unindent()
                                .add("} else {\n").indent()
                                .add("$L.set$L" + generatorId(idEle) + "\n", entityParam, id, IdGenerator.class)
                                .add("save = this.save($L);\n", entityParam).unindent()
                                .add("}\n")
                                .add("return save ? $L : null", entityParam)
                                .build())
                        .returns(TypeName.get(classTypeMirror))
                        .build())
                .addMethod(MethodSpec.methodBuilder("saveBatch" + simpleClassName)
                        .addJavadoc("基础设施层应用已生成批量保存实体$L方法:$L\n\n", simpleClassName, "saveBatch" + simpleClassName)
                        .addJavadoc("@param $L 仓储层实体列表$L\n", entityParam, entityParam)
                        .addJavadoc("@return {@link $L} 成功保存的仓储层实体列表$L", simpleClassName, entityParam)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(classTypeMirror)), entityParam)
                        .addStatement(CodeBlock.builder().indent()
                                .beginControlFlow("if ($T.isNull($L) || $L.isEmpty())", Objects.class, entityParam, entityParam)
                                .add("throw $T.build($T.BASIC_ERROR, \"仓储层实体列表为空~\");\n", BusinessException.class, AdviceErrorCode.class)
                                .unindent().endControlFlow()
                                .beginControlFlow("$L.forEach(e ->", entityParam)
                                .add("e.set$L" + generatorId(idEle) + "\n", id, IdGenerator.class)
                                .endControlFlow(")")
                                .add("boolean success = this.getBaseMapper().insertBatch$L($L);\n", simpleClassName, entityParam)
                                .beginControlFlow("if (!success)")
                                .add("throw BusinessException.build(AdviceErrorCode.BASIC_ERROR, \"批量保存失败了,请稍后重试~\");\n")
                                .endControlFlow()
                                .add("return $L", entityParam)
                                .build())
                        .returns(ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(classTypeMirror)))
                        .build())
                .addMethod(MethodSpec.methodBuilder("select" + simpleClassName + "ByIds")
                        .addJavadoc("基础设施层应用已生成批量根据id查询实体$L方法:$L\n\n", simpleClassName, "select" + simpleClassName + "ByIds")
                        .addJavadoc("@param $L 仓储层实体主键id列表$L\n", entityParam, entityParam)
                        .addJavadoc("@return {@link $L} 查询到的仓储层实体$L列表", simpleClassName, entityParam)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), idType), "ids")
                        .addStatement(CodeBlock.builder().indent()
                                .add("return this.listByIds(ids)").unindent()
                                .build())
                        .returns(ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(classTypeMirror)))
                        .build())
                .addMethod(MethodSpec.methodBuilder("selectAll" + simpleClassName)
                        .addJavadoc("基础设施层应用已生成查询所有实体$L列表方法:$L\n\n", simpleClassName, "selectAll" + simpleClassName)
                        .addJavadoc("@return {@link $L} 查询到的仓储层实体$L列表", simpleClassName, entityParam)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addStatement(CodeBlock.builder().indent()
                                .add("return this.list()").unindent()
                                .build())
                        .returns(ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(classTypeMirror)))
                        .build())
                .addMethod(MethodSpec.methodBuilder("selectList" + simpleClassName)
                        .addJavadoc("基础设施层应用已生成根据条件查询实体$L列表方法:$L\n\n", simpleClassName, "selectList" + simpleClassName)
                        .addJavadoc("@param $L 仓储层实体\n", entityParam)
                        .addJavadoc("@return {@link $L} 查询到的仓储层实体$L列表", simpleClassName, entityParam)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(TypeName.get(classTypeMirror), entityParam)
                        .addStatement(CodeBlock.builder().indent()
                                .add("return this.list($L)", queryParamCodeBlock)
                                .unindent().unindent().unindent().unindent().unindent()
                                .build())
                        .returns(ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(classTypeMirror)))
                        .build())
                .addMethod(MethodSpec.methodBuilder("count" + simpleClassName)
                        .addJavadoc("基础设施层应用已生成查询实体$L数量方法:$L\n\n", simpleClassName, "count" + simpleClassName)
                        .addJavadoc("@return {@link $L} 查询到的仓储层实体$L数量", simpleClassName, entityParam)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addStatement(CodeBlock.builder().indent()
                                .add("return this.count()").unindent()
                                .build())
                        .returns(TypeName.INT.box())
                        .build())
                .addMethod(MethodSpec.methodBuilder("count" + simpleClassName)
                        .addJavadoc("基础设施层应用已生成根据条件查询实体$L数量方法:$L\n\n", simpleClassName, "count" + simpleClassName)
                        .addJavadoc("@param $L 仓储层实体\n", entityParam)
                        .addJavadoc("@return {@link $L} 查询到的仓储层实体$L数量", simpleClassName, entityParam)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(TypeName.get(classTypeMirror), entityParam)
                        .addStatement(CodeBlock.builder().indent()
                                .add("return this.count($L)", queryParamCodeBlock)
                                .unindent().unindent()
                                .unindent().unindent().unindent()
                                .build())
                        .returns(TypeName.INT.box())
                        .build())
                .addMethod(MethodSpec.methodBuilder("pageSelect" + simpleClassName)
                        .addJavadoc("基础设施层应用已生成分页查询实体$L方法:$L\n\n", simpleClassName, "pageSelect" + simpleClassName)
                        .addJavadoc("@param $L 仓储层实体\n", entityParam)
                        .addJavadoc("@param pageNo 当前页\n")
                        .addJavadoc("@param pageSize 每页条数\n")
                        .addJavadoc("@return {@link $L} {@link $T} 查询到的仓储层实体$L分页结果", simpleClassName, Page.class, entityParam)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(TypeName.get(classTypeMirror), entityParam)
                        .addParameter(ParameterSpec.builder(TypeVariableName.get(Integer.class), "pageNo").build())
                        .addParameter(ParameterSpec.builder(TypeVariableName.get(Integer.class), "pageSize").build())
                        .addStatement(CodeBlock.builder().indent()
                                .add("$T<$T> page = new $T<>(pageNo, pageSize);\n", Page.class, TypeName.get(classTypeMirror), Page.class)
                                .unindent()
                                .beginControlFlow("if ($T.isNull($L))", Objects.class, entityParam)
                                .add("page = this.page(page, new $T<$T>());\n", LambdaQueryWrapper.class, TypeName.get(classTypeMirror))
                                .nextControlFlow("else")
                                .add("page = this.page(page, $L);\n", queryParamCodeBlock)
                                .unindent()
                                .endControlFlow()
                                .add("return page")
                                .unindent().unindent().unindent()
                                .build())
                        .returns(ParameterizedTypeName.get(ClassName.get(Page.class), TypeName.get(classTypeMirror)))
                        .build())
                .addMethod(MethodSpec.methodBuilder("remove" + simpleClassName + "ByIds")
                        .addJavadoc("基础设施层应用已生成批量根据id删除实体$L方法:$L\n\n", simpleClassName, "remove" + simpleClassName + "ByIds")
                        .addJavadoc("@param ids 仓储层实体$L主键列表\n", simpleClassName)
                        .addJavadoc("@return 是否删除成功")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(Long.class)), "ids")
                        .addStatement(CodeBlock.builder().indent()
                                .add("return this.removeByIds(ids)").unindent()
                                .build())
                        .returns(TypeName.get(Boolean.class))
                        .build())
                .addMethod(MethodSpec.methodBuilder("remove" + simpleClassName + "ById")
                        .addJavadoc("基础设施层应用已生成根据id删除实体$L方法:$L\n\n", simpleClassName, "remove" + simpleClassName + "ById")
                        .addJavadoc("@param ids 仓储层实体$L主键列表\n", simpleClassName)
                        .addJavadoc("@return 是否删除成功")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(TypeName.get(Long.class), "id")
                        .addStatement(CodeBlock.builder()
                                .add("return this.removeById(id)")
                                .build())
                        .returns(TypeName.get(Boolean.class))
                        .build())
                .addMethod(MethodSpec.methodBuilder("remove" + simpleClassName)
                        .addJavadoc("基础设施层应用已生成根据条件删除实体$L方法:$L\n\n", simpleClassName, "remove" + simpleClassName)
                        .addJavadoc("@param $L 仓储层实体$L\n", entityParam, simpleClassName)
                        .addJavadoc("@return 是否删除成功")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(TypeName.get(classTypeMirror), entityParam)
                        .addStatement(CodeBlock.builder()
                                .add("return this.remove($L)", queryParamCodeBlock)
                                .unindent().unindent().unindent().unindent()
                                .build())
                        .returns(TypeName.BOOLEAN.box())
                        .build());
        //判断字段注解是否存在根据不同注解生成不同方法
        generatorQueryByFieldInterfaceMethodImpl(classElement, classBuilder, classTypeMirror, entityParam, simpleClassName);
        generatorUpdateByFieldInterfaceMethodImpl(classElement, classBuilder, classTypeMirror, entityParam, simpleClassName);
        generatorRemoveByFieldInterfaceMethodImpl(classElement, classBuilder, classTypeMirror, entityParam, simpleClassName);
        return classBuilder.build();
    }

    /**
     * 生成根据字段删除接口方法定义
     */
    private void generatorRemoveByFieldInterfaceMethod(TypeElement classElement, TypeSpec.Builder classBuilder, TypeMirror classTypeMirror, String entityParam, String simpleClassName) {
        List<? extends Element> dataBaseFieldsByClass = getDataBaseFieldsByClass(classElement);
        for (Element element : dataBaseFieldsByClass) {
            GeneratorRemoveByThisField annotation = element.getAnnotation(GeneratorRemoveByThisField.class);
            String firstCharToUpperCase = firstCharToUpperCase(element.getSimpleName().toString());
            String firstCharToLowerCase = firstCharToLowerCase(element.getSimpleName().toString());
            if (Objects.nonNull(annotation)) {
                boolean batch = annotation.batch();
                TypeName typeName;
                if (batch) {
                    typeName = ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(element.asType()));
                } else {
                    typeName = TypeName.get(element.asType());
                }
                classBuilder.addMethod(MethodSpec.methodBuilder("removeBy" + firstCharToUpperCase)
                        .addJavadoc("基础设施层应用已生成根据$L字段删除实体$L方法:$L\n\n", element.getSimpleName(), simpleClassName, "removeBy" + firstCharToUpperCase)
                        .addJavadoc("@param $L 仓储层实体字段\n", firstCharToLowerCase)
                        .addJavadoc("@return 是否删除成功")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(typeName, firstCharToLowerCase)
                        .returns(TypeName.BOOLEAN.box())
                        .build());
            }
        }
    }

    private void generatorRemoveByFieldInterfaceMethodImpl(TypeElement classElement, TypeSpec.Builder classBuilder, TypeMirror classTypeMirror, String entityParam, String simpleClassName) {
        List<? extends Element> dataBaseFieldsByClass = getDataBaseFieldsByClass(classElement);
        for (Element element : dataBaseFieldsByClass) {
            GeneratorRemoveByThisField annotation = element.getAnnotation(GeneratorRemoveByThisField.class);
            String firstCharToUpperCase = firstCharToUpperCase(element.getSimpleName().toString());
            String firstCharToLowerCase = firstCharToLowerCase(element.getSimpleName().toString());

            if (Objects.nonNull(annotation)) {
                boolean batch = annotation.batch();
                TypeName typeName;

                CodeBlock.Builder codeBlockBuilder = CodeBlock.builder().indent();
                if (batch) {
                    typeName = ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(element.asType()));
                    codeBlockBuilder.beginControlFlow("if ($T.isNull($L) || $L.isEmpty())", Objects.class, firstCharToLowerCase, firstCharToLowerCase)
                            .add("throw $T.build($T.BASIC_ERROR, \"删除依据字段不能为空~\");\n", BusinessException.class, AdviceErrorCode.class)
                            .unindent().unindent()
                            .endControlFlow();
                } else {
                    typeName = TypeName.get(element.asType());
                    //判空逻辑
                    if ("java.lang.String".equals(element.asType().toString())) {
                        codeBlockBuilder.beginControlFlow("if ($T.isBlank($L))", StringUtils.class, firstCharToLowerCase)
                                .add("throw $T.build($T.BASIC_ERROR, \"删除依据字段不能为空~\");\n", BusinessException.class, AdviceErrorCode.class)
                                .unindent().unindent()
                                .endControlFlow();
                    } else {
                        codeBlockBuilder.beginControlFlow("if ($T.isNull($L))", Objects.class, firstCharToLowerCase)
                                .add("throw $T.build($T.BASIC_ERROR, \"删除依据字段不能为空~\");\n", BusinessException.class, AdviceErrorCode.class)
                                .unindent().unindent()
                                .endControlFlow();
                    }
                }

                codeBlockBuilder.add("return this.remove($L)", generatorRemoveCodeBlock(element, classTypeMirror, annotation.batch(), firstCharToLowerCase))
                        .unindent().unindent();
                classBuilder.addMethod(MethodSpec.methodBuilder("removeBy" + firstCharToUpperCase)
                        .addJavadoc("基础设施层应用已生成根据$L字段删除实体$L方法:$L\n\n", element.getSimpleName(), simpleClassName, "removeBy" + firstCharToUpperCase)
                        .addJavadoc("@param $L 仓储层实体字段\n", firstCharToLowerCase)
                        .addJavadoc("@return 是否删除成功")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(typeName, firstCharToLowerCase)
                        .addStatement(codeBlockBuilder.build())
                        .returns(TypeName.BOOLEAN.box())
                        .build());
            }
        }
    }

    private void generatorUpdateByFieldInterfaceMethod(TypeElement classElement, TypeSpec.Builder classBuilder, TypeMirror classTypeMirror, String entityParam, String simpleClassName) {
        List<? extends Element> dataBaseFieldsByClass = getDataBaseFieldsByClass(classElement);
        for (Element element : dataBaseFieldsByClass) {
            GeneratorUpdateByThisField annotation = element.getAnnotation(GeneratorUpdateByThisField.class);
            String firstCharToUpperCase = firstCharToUpperCase(element.getSimpleName().toString());
            if (Objects.nonNull(annotation)) {
                classBuilder.addMethod(MethodSpec.methodBuilder("updateBy" + firstCharToUpperCase)
                        .addJavadoc("基础设施层应用已生成根据$L字段更新实体$L方法:$L\n\n", element.getSimpleName(), simpleClassName, "updateBy" + firstCharToUpperCase)
                        .addJavadoc("@param $L 仓储层实体$L\n", entityParam, simpleClassName)
                        .addJavadoc("@return 是否更新成功")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(TypeName.get(classTypeMirror), entityParam)
                        .returns(TypeName.BOOLEAN.box())
                        .build());
            }
        }
    }

    private void generatorUpdateByFieldInterfaceMethodImpl(TypeElement classElement, TypeSpec.Builder classBuilder, TypeMirror classTypeMirror, String entityParam, String simpleClassName) {
        List<? extends Element> dataBaseFieldsByClass = getDataBaseFieldsByClass(classElement);
        for (Element element : dataBaseFieldsByClass) {
            GeneratorUpdateByThisField annotation = element.getAnnotation(GeneratorUpdateByThisField.class);
            String firstCharToUpperCase = firstCharToUpperCase(element.getSimpleName().toString());
            if (Objects.nonNull(annotation)) {
                CodeBlock.Builder codeBlockBuilder = CodeBlock.builder().indent();
                codeBlockBuilder.beginControlFlow("if ($T.isNull($L))", Objects.class, entityParam)
                        .add("throw $T.build($T.BASIC_ERROR, \"更新实体不能为空~\");\n", BusinessException.class, AdviceErrorCode.class)
                        .unindent().unindent()
                        .endControlFlow();
                //判空逻辑
                if ("java.lang.String".equals(element.asType().toString())) {
                    codeBlockBuilder.beginControlFlow("if ($T.isBlank($L.get$L()))", StringUtils.class, entityParam, firstCharToUpperCase)
                            .add("throw $T.build($T.BASIC_ERROR, \"更新依据字段不能为空~\");\n", BusinessException.class, AdviceErrorCode.class)
                            .endControlFlow();
                } else {
                    codeBlockBuilder.beginControlFlow("if ($T.isNull($L.get$L()))", Objects.class, entityParam, firstCharToUpperCase)
                            .add("throw $T.build($T.BASIC_ERROR, \"更新依据字段不能为空~\");\n", BusinessException.class, AdviceErrorCode.class)
                            .endControlFlow();
                }
                codeBlockBuilder.add("return this.update($L,$L)", entityParam, generatorQueryCodeBlock(Collections.singletonList(element), classTypeMirror, entityParam, true, true, false))
                        .unindent().unindent().unindent();
                classBuilder.addMethod(MethodSpec.methodBuilder("updateBy" + firstCharToUpperCase)
                        .addJavadoc("基础设施层应用已生成根据$L字段更新实体$L方法:$L\n\n", element.getSimpleName(), simpleClassName, "updateBy" + firstCharToUpperCase)
                        .addJavadoc("@param $L 仓储层实体$L\n", entityParam, simpleClassName)
                        .addJavadoc("@return 是否更新成功")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(TypeName.get(classTypeMirror), entityParam)
                        .addStatement(codeBlockBuilder.build())
                        .returns(TypeName.BOOLEAN.box())
                        .build());
            }
        }
    }

    private void generatorQueryByFieldInterfaceMethod(TypeElement classElement, TypeSpec.Builder classBuilder, TypeMirror classTypeMirror, String entityParam, String simpleClassName) {
        List<? extends Element> dataBaseFieldsByClass = getDataBaseFieldsByClass(classElement);
        for (Element element : dataBaseFieldsByClass) {
            GeneratorQueryByThisField annotation = element.getAnnotation(GeneratorQueryByThisField.class);
            String firstCharToUpperCase = firstCharToUpperCase(element.getSimpleName().toString());
            String firstCharToLowerCase = firstCharToLowerCase(element.getSimpleName().toString());
            if (Objects.nonNull(annotation)) {
                TypeName resultType;
                TypeName paramType;
                boolean batch = annotation.batch();
                if (batch) {
                    paramType = ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(element.asType()));
                    resultType = ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(classTypeMirror));
                } else {
                    paramType = TypeName.get(element.asType());
                    //获取结果类型
                    if (annotation.resultType().equals(ResultType.LIST)) {
                        resultType = ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(classTypeMirror));
                    } else {
                        resultType = TypeName.get(classTypeMirror);
                    }
                }
                classBuilder.addMethod(MethodSpec.methodBuilder("selectBy" + firstCharToUpperCase)
                        .addJavadoc("基础设施层应用已生成根据$L字段查询实体$L方法:$L\n\n", element.getSimpleName(), simpleClassName, "selectBy" + firstCharToUpperCase)
                        .addJavadoc("@param $L 仓储层实体字段$L\n", firstCharToLowerCase, firstCharToLowerCase)
                        .addJavadoc("@return {@link $L} 查询到的仓储层实体$L列表", simpleClassName, entityParam)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(paramType, firstCharToLowerCase)
                        .returns(resultType)
                        .build());
            }
        }
    }

    private void generatorQueryByFieldInterfaceMethodImpl(TypeElement classElement, TypeSpec.Builder classBuilder, TypeMirror classTypeMirror, String param, String simpleName) {
        List<? extends Element> dataBaseFieldsByClass = getDataBaseFieldsByClass(classElement);
        for (Element element : dataBaseFieldsByClass) {
            GeneratorQueryByThisField annotation = element.getAnnotation(GeneratorQueryByThisField.class);
            String firstCharToUpperCase = firstCharToUpperCase(element.getSimpleName().toString());
            String firstCharToLowerCase = firstCharToLowerCase(element.getSimpleName().toString());
            if (Objects.nonNull(annotation)) {
                TypeName resultType;
                TypeName paramType;
                CodeBlock.Builder codeBlockBuilder = CodeBlock.builder().indent();
                boolean batch = annotation.batch();
                if (batch) {
                    paramType = ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(element.asType()));
                    resultType = ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(classTypeMirror));
                    codeBlockBuilder.beginControlFlow("if ($T.isNull($L) || $L.isEmpty())", Objects.class, firstCharToLowerCase, firstCharToLowerCase)
                            .add("throw $T.build($T.BASIC_ERROR, \"查询依据字段不能为空~\");\n", BusinessException.class, AdviceErrorCode.class)
                            .unindent().unindent()
                            .endControlFlow();
                    codeBlockBuilder.add("return this.list($L)", generatorQueryCodeBlock(Collections.singletonList(element), classTypeMirror, firstCharToLowerCase, false, false, true));
                } else {
                    paramType = TypeName.get(element.asType());
                    //判空逻辑
                    if ("java.lang.String".equals(element.asType().toString())) {
                        codeBlockBuilder.beginControlFlow("if ($T.isBlank($L))", StringUtils.class, firstCharToLowerCase)
                                .add("throw $T.build($T.BASIC_ERROR, \"查询依据字段不能为空~\");\n", BusinessException.class, AdviceErrorCode.class)
                                .unindent().unindent()
                                .endControlFlow();
                    } else {
                        codeBlockBuilder.beginControlFlow("if ($T.isNull($L))", Objects.class, firstCharToLowerCase)
                                .add("throw $T.build($T.BASIC_ERROR, \"查询依据字段不能为空~\");\n", BusinessException.class, AdviceErrorCode.class)
                                .unindent().unindent()
                                .endControlFlow();
                    }
                    //获取结果类型
                    if (annotation.resultType().equals(ResultType.LIST)) {
                        resultType = ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(classTypeMirror));
                        codeBlockBuilder.add("return this.list($L)", generatorQueryCodeBlock(Collections.singletonList(element), classTypeMirror, firstCharToLowerCase, false, false, false));
                    } else {
                        resultType = TypeName.get(classTypeMirror);
                        codeBlockBuilder.add("return this.getOne($L)", generatorQueryCodeBlock(Collections.singletonList(element), classTypeMirror, firstCharToLowerCase, false, false, false));
                    }
                }

                codeBlockBuilder.unindent().unindent().unindent();

                classBuilder.addMethod(MethodSpec.methodBuilder("selectBy" + firstCharToUpperCase)
                        .addJavadoc("基础设施层应用已生成根据$L字段查询实体$L方法:$L\n\n", element.getSimpleName(), simpleName, "selectBy" + firstCharToUpperCase)
                        .addJavadoc("@param $L 仓储层实体字段$L\n", firstCharToLowerCase, firstCharToLowerCase)
                        .addJavadoc("@return {@link $L} 查询到的仓储层实体$L列表", simpleName, param)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addStatement(codeBlockBuilder.build())
                        .addParameter(paramType, firstCharToLowerCase)
                        .returns(resultType)
                        .build());
            }
        }
    }

    private CodeBlock generatorRemoveCodeBlock(Element fieldElement, TypeMirror typeMirror, boolean batch, String entityParam) {
        String field = firstCharToUpperCase(fieldElement.getSimpleName().toString());
        CodeBlock.Builder codeBuilder = CodeBlock.builder()
                .add("new $T<$T>()\n", LambdaQueryWrapper.class, TypeName.get(typeMirror))
                .indent().indent().indent().indent();
        if (batch) {
            codeBuilder.add(".in($T::get$L, $L)\n", typeMirror, field, entityParam);
        } else {
            codeBuilder.add(".eq($T::get$L, $L)\n", typeMirror, field, entityParam);
        }
        return codeBuilder.build();
    }

    /**
     * 生成查询条件代码块
     */
    private CodeBlock generatorQueryCodeBlock(List<? extends Element> fieldElements, TypeMirror typeMirror, String param, boolean determine, boolean update, boolean batch) {
        CodeBlock.Builder codeBuilder = CodeBlock.builder()
                .add("new $T<$T>()\n", LambdaQueryWrapper.class, TypeName.get(typeMirror))
                .indent().indent().indent().indent();
        for (Element element : fieldElements) {
            TypeMirror type = element.asType();
            String field = firstCharToUpperCase(element.getSimpleName().toString());
            if (batch) {
                codeBuilder.add(".in($T::get$L, $L)\n", typeMirror, field, param);
                continue;
            }
            if (TypeName.get(type).equals(TypeName.INT.box())
                    || TypeName.get(type).equals(TypeName.BOOLEAN.box())
                    || TypeName.get(type).equals(TypeName.LONG.box())
                    || TypeName.get(type).equals(TypeName.CHAR.box())
                    || TypeName.get(type).equals(TypeName.BYTE.box())
                    || TypeName.get(type).equals(TypeName.DOUBLE.box())
                    || TypeName.get(type).equals(TypeName.FLOAT.box())
                    || TypeName.get(type).equals(TypeName.SHORT.box())
            ) {
                if (determine) {
                    codeBuilder.add(".eq($L.get$L() != null, $T::get$L, $L.get$L())\n", param, field, typeMirror, field, param, field);
                } else {
                    codeBuilder.add(".eq($T::get$L, $L)\n", typeMirror, field, param);
                }
            }
            if ("java.lang.String".equals(type.toString())) {
                if (determine) {
                    if (update) {
                        codeBuilder.add(".eq($T.isNotBlank($L.get$L()), $T::get$L, $L.get$L())\n", StringUtils.class, param, field, typeMirror, field, param, field);
                    } else {
                        codeBuilder.add(".likeRight($T.isNotBlank($L.get$L()), $T::get$L, $L.get$L())\n", StringUtils.class, param, field, typeMirror, field, param, field);
                    }
                } else {
                    codeBuilder.add(".likeRight($T::get$L, $L)\n", typeMirror, field, param);
                }
            }
        }
        return codeBuilder.build();
    }

    /**
     * 查找主键字段,默认id
     */
    private Element getId(List<? extends Element> fieldElements) {
        for (Element element : fieldElements) {
            TableId tableId = element.getAnnotation(TableId.class);
            if (tableId != null) {
                return element;
            }
        }
        return null;
    }

    private String getTableName(TypeElement classElement) {
        TableName annotation = classElement.getAnnotation(TableName.class);
        if (null != annotation) {
            return annotation.value();
        }
        String className = classElement.getSimpleName().toString();
        return convertToSnakeCase(className);
    }

    /**
     * 主键生成(类型转换)
     */
    private String generatorId(Element element) {
        if (null == element) {
            return "($T.ins().generator());";
        }
        TypeMirror typeMirror = element.asType();
        TypeName typeName = TypeName.get(typeMirror);
        if (typeName.equals(TypeName.INT.box())) {
            return "($T.ins().generator().intValue());";
        }
        if (typeName.equals(TypeName.LONG.box())) {
            return "($T.ins().generator());";
        }
        if ("java.lang.String".equals(typeMirror.toString())) {
            return "($T.ins().generator().toString());";
        }
        return "($T.ins().generator()); //主键类型仅支持:String、Integer、Long";
    }


    /**
     * 遍历查找类字段
     */
    private List<? extends Element> getClassFields(TypeElement classElement) {
        //获取子元素
        List<? extends Element> enclosedElements = classElement.getEnclosedElements();
        return enclosedElements.stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .collect(Collectors.toList());
    }

    /**
     * 从类中获取数据库字段元素
     */
    private List<? extends Element> getDataBaseFieldsByClass(TypeElement classElement) {
        //获取子元素
        List<? extends Element> enclosedElements = classElement.getEnclosedElements();
        List<? extends Element> classFields = enclosedElements.stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .collect(Collectors.toList());
        List<Element> databaseFields = new ArrayList<>();
        for (Element ele : classFields) {
            //剔除不存在的字段
            TableField tableField = ele.getAnnotation(TableField.class);
            if (null != tableField) {
                if (tableField.exist()) {
                    databaseFields.add(ele);
                }
            } else {
                databaseFields.add(ele);
            }
        }
        return databaseFields;
    }

    /**
     * 字段转数据库字段字段
     */
    private String fieldsToDatabaseFields(List<String> fields) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            stringBuilder.append("`").append(fields.get(i)).append("`");
            if (i < fields.size() - 1) {
                stringBuilder.append(",");
            }
            if (i % 5 == 0 && i > 1) {
                stringBuilder.append("\n\t\t");
            }
        }
        return stringBuilder.toString();
    }

    private String fieldsToInsertFields(List<String> fields) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        for (int i = 0; i < fields.size(); i++) {
            stringBuilder.append("#{item.").append(fields.get(i)).append("}");
            if (i < fields.size() - 1) {
                stringBuilder.append(", ");
            }
            if (i % 5 == 0 && i > 1) {
                stringBuilder.append("\n\t\t\t");
            }
        }
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    /**
     * 首字母转换为小写
     */
    public static String firstCharToLowerCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        char firstChar = Character.toLowerCase(str.charAt(0));
        if (str.length() == 1) {
            return String.valueOf(firstChar);
        }
        return firstChar + str.substring(1);
    }

    /**
     * 首字母转换为大写
     */
    public static String firstCharToUpperCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        char firstChar = Character.toUpperCase(str.charAt(0));
        if (str.length() == 1) {
            return String.valueOf(firstChar);
        }
        return firstChar + str.substring(1);
    }

    /**
     * 驼峰转下划线
     */
    public static String convertToSnakeCase(String input) {
        Matcher matcher = HUMP_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }


}
