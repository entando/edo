/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.edo.builder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.entando.edo.builder.out.EdoReportEntry;
import org.entando.edo.model.EdoBean;
import org.entando.edo.model.EdoBuilder;
import org.entando.edo.model.EdoConstants;
import org.entando.edo.report.Report;
import org.entando.edo.template.Render;
import org.entando.edo.template.Templates;


public class Builder {

    public static Logger logger = LogManager.getLogger(Builder.class);


    public void build(EdoBuilder builder) throws Throwable {


        EdoBean bean = builder.getBean();
        Render render = new Render();
        Map<String, Object> contextElements = new HashMap<String, Object>();
        contextElements.put("bean", bean);
        contextElements.put("constants", new EdoConstants());

        this.writeJavaModel(render, contextElements, bean);
        this.writeSpringModelXml(render, contextElements, bean);
        this.writeEntantoInit(render, contextElements, bean);

        if (builder.getAssetsConf().isRest()) {
            this.writeService(render, contextElements, bean);
            this.writeRestController(render, contextElements, bean);
        }

        if (builder.getAssetsConf().isCxf()) {
            this.writeCxfAPI(render, contextElements, bean);
        }


        //

        if (builder.getAssetsConf().isSpecialWidget()) {
            this.writeSpecilWidget(render, contextElements, bean);
        }

        if (builder.getAssetsConf().isInternalServlet()) {
            this.writeInternalServlet(render, contextElements, bean);
        }

        if (builder.getAssetsConf().isAdminConsole() || builder.getAssetsConf().isInternalServlet()) {
            this.writeStruts2Action(render, contextElements, bean);

        }

        if (builder.getAssetsConf().isAdminConsole()) {
            this.writeStruts2Jsp(render, contextElements, bean);
        }

        if (builder.getAssetsConf().isSpecialWidget() || builder.getAssetsConf().isAdminConsole() || builder.getAssetsConf().isInternalServlet()) {
            //STRUTS_PLUGIN
            String strutsPluginFilePath = ControllerFileBuilder.getActionStrutsPluginFilePath(bean);
            String actionStrutsPluginContent = render.render(Templates.ACTION_STRUTS_PLUGIN, contextElements);
            this.writeFile(bean.getEdoBuilder(), strutsPluginFilePath, actionStrutsPluginContent);

            //SPRING
            String springFilePath = ControllerFileBuilder.getActionSpringFilePath(bean);
            String actionSpringContent = render.render(Templates.ACTION_SPRING, contextElements);
            this.writeFile(bean.getEdoBuilder(), springFilePath, actionSpringContent);
        }


        this.writeTestService(render, contextElements, bean);
        this.writeTestAction(render, contextElements, bean);

        this.saveReport(render, contextElements, bean);
        builder.getEdoWriter().onClose();
    }

    /**
     * Scrive sulla root il report dell'esecuzione
     * @param render
     * @param contextElements
     * @param bean
     */
    private void saveReport(Render render, Map<String, Object> contextElements, EdoBean bean) {
        try {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String nowString = sdf.format(cal.getTime());

            contextElements.put("report", Report.getInstance());
            String text = render.render(Templates.EDO_REPORT, contextElements);
            String filename = "edo_" + nowString + "_" + bean.getName() + "-report.txt";
            bean.getEdoBuilder().getEdoWriter().write(filename, text);
        } catch (Throwable t) {
            logger.error("error!", t);
        }
    }

    private void writeJavaModel(Render render, Map<String, Object> contextElements, EdoBean bean) {
        try {
            //POJO
            String pojopath = Filebuilder.getPojoFilePath(bean);
            String pojo = render.render(Templates.BEAN, contextElements);
            this.writeFile(bean.getEdoBuilder(), pojopath, pojo);

            //DAO_INTERFACE
            String daoInterfacePath = Filebuilder.getDaoInterfaceFilePath(bean);
            String daoInterface = render.render(Templates.DAO_INTERFACE, contextElements);
            this.writeFile(bean.getEdoBuilder(), daoInterfacePath, daoInterface);

            //DAO
            String daoImplPath = Filebuilder.getDaoImplFilePath(bean);
            String daoImpl = render.render(Templates.DAO_IMPL, contextElements);
            this.writeFile(bean.getEdoBuilder(), daoImplPath, daoImpl);

            //MANAGER_INTERFACE
            String managerIterfacePath = Filebuilder.getManagerInterfaceFilePath(bean);
            String managerInterface = render.render(Templates.MANAGER_INTERFACE, contextElements);
            this.writeFile(bean.getEdoBuilder(), managerIterfacePath, managerInterface);

            //MANAGER_IMPL
            String managerImplPath = Filebuilder.getManagerImplFilePath(bean);
            String manager = render.render(Templates.MANAGER_IMPL, contextElements);
            this.writeFile(bean.getEdoBuilder(), managerImplPath, manager);

            //EVENT
            String eventPath = Filebuilder.getEventFilePath(bean);
            String event = render.render(Templates.EVENT, contextElements);
            this.writeFile(bean.getEdoBuilder(), eventPath, event);

            String observerPath = Filebuilder.getObserverFilePath(bean);
            String observer = render.render(Templates.EVENT_OBSERVER, contextElements);
            this.writeFile(bean.getEdoBuilder(), observerPath, observer);

        } catch (Throwable t) {
            logger.error("error writing files for {}", bean.getName(), t);
        }
    }

    private void writeCxfAPI(Render render, Map<String, Object> contextElements, EdoBean bean) {
        try {
            //jaxbbean
            String apiBeanPath = Filebuilder.getApiBeanFilePath(bean);
            String apiBean = render.render(Templates.API_BEAN, contextElements);
            this.writeFile(bean.getEdoBuilder(), apiBeanPath, apiBean);

            //api response
            String apiResponsePath = Filebuilder.getApiResponseFilePath(bean);
            String apiResponse = render.render(Templates.API_RESPONSE, contextElements);
            this.writeFile(bean.getEdoBuilder(), apiResponsePath, apiResponse);

            //api response result
            String apiResponseResultPath = Filebuilder.getApiResponseResultFilePath(bean);
            String apiResponseResult = render.render(Templates.API_RESPONSE_RESULT, contextElements);
            this.writeFile(bean.getEdoBuilder(), apiResponseResultPath, apiResponseResult);


            //api LIST response
            String apiListResponsePath = Filebuilder.getApiListResponseFilePath(bean);
            String apiListResponse = render.render(Templates.API_LIST_RESPONSE, contextElements);
            this.writeFile(bean.getEdoBuilder(), apiListResponsePath, apiListResponse);

            //api LIST response result
            String apiListResponseResultPath = Filebuilder.getApiListResponseResultFilePath(bean);
            String apiListResponseResult = render.render(Templates.API_LIST_RESPONSE_RESULT, contextElements);
            this.writeFile(bean.getEdoBuilder(), apiListResponseResultPath, apiListResponseResult);


            //api methods
            String apiMethodsPath = Filebuilder.getApiMethodsFilePath(bean);
            String apiMethods = render.render(Templates.API_METHODS_XML, contextElements);
            this.writeFile(bean.getEdoBuilder(), apiMethodsPath, apiMethods);

        } catch (Throwable t) {
            logger.error("error!", t);
        }

    }

    private void writeSpringModelXml(Render render, Map<String, Object> contextElements, EdoBean bean) {
        try {
            String stringPath = Filebuilder.getSpringManagerFilePath(bean);
            String xmlContent = render.render(Templates.SPRING_MANAGERS_CONFIG_XML, contextElements);
            this.writeFile(bean.getEdoBuilder(), stringPath, xmlContent);
        } catch (Throwable t) {
            logger.error("error writing files for {}", bean.getName(), t);
        }
    }


    private void writeEntantoInit(Render render, Map<String, Object> contextElements, EdoBean bean) {
        try {
            //POJO
            String pojoPath = Filebuilder.getEntandoPojoInitFilePath(bean);
            String javaContent = render.render(Templates.ENTANDO_INIT_POJO, contextElements);
            this.writeFile(bean.getEdoBuilder(), pojoPath, javaContent);

            //COMPONENT
            String componentPath = Filebuilder.getEntandoComponentFilePath(bean);
            String xmlContent = render.render(Templates.ENTANDO_INIT_COMPONENT_XML, contextElements);
            this.writeFile(bean.getEdoBuilder(), componentPath, xmlContent);

            //PROD SQL SERV
            String sqlServPath = Filebuilder.getEntandoComponentSqlServProductionFilePath(bean);
            String sqlServContent = render.render(Templates.ENTANDO_INIT_SERV_DATA_PRODUCTION_SQL, contextElements);
            this.writeFile(bean.getEdoBuilder(), sqlServPath, sqlServContent);
            //PROD SQL PORT
            String sqlPortPath = Filebuilder.getEntandoComponentSqlPortProductionFilePath(bean);
            String sqlPortContent = render.render(Templates.ENTANDO_INIT_PORT_DATA_PRODUCTION_SQL, contextElements);
            this.writeFile(bean.getEdoBuilder(), sqlPortPath, sqlPortContent);

            //TEST SQL SERV
            String sqlTestServPath = Filebuilder.getEntandoComponentSqlServTestFilePath(bean);
            String sqlTestServContent = render.render(Templates.ENTANDO_INIT_SERV_DATA_TEST_SQL, contextElements);
            this.writeFile(bean.getEdoBuilder(), sqlTestServPath, sqlTestServContent);
            //TEST SQL PORT
            String sqlTestPortPath = Filebuilder.getEntandoComponentSqlPortTestFilePath(bean);
            String sqlTestPortContent = render.render(Templates.ENTANDO_INIT_PORT_DATA_TEST_SQL, contextElements);
            this.writeFile(bean.getEdoBuilder(), sqlTestPortPath, sqlTestPortContent);

        } catch (Throwable t) {
            logger.error("error writing files for {}", bean.getName(), t);
        }
    }

    private void writeRestController(Render render, Map<String, Object> contextElements, EdoBean bean) throws Throwable {
        //dtorequest
        String pojoPath = ServiceFileBuilder.getDtoRequestFilePath(bean);
        String javaContent = render.render(Templates.BEAN_DTO_REQUEST, contextElements);
        this.writeFile(bean.getEdoBuilder(), pojoPath, javaContent);

        //validator
        String validatorPath = ServiceFileBuilder.getControllerValidatorFilePath(bean);
        String validatorContent = render.render(Templates.BEAN_CONTROLLER_VALIDATOR, contextElements);
        this.writeFile(bean.getEdoBuilder(), validatorPath, validatorContent);

        //Controller
        String controllerPath = ServiceFileBuilder.getControllerFilePath(bean);
        String controllerContent = render.render(Templates.BEAN_CONTROLLER, contextElements);
        this.writeFile(bean.getEdoBuilder(), controllerPath, controllerContent);

    }

    private void writeService(Render render, Map<String, Object> contextElements, EdoBean bean) {
        try {
            //dto
            String pojoPath = ServiceFileBuilder.getDtoFilePath(bean);
            String javaContent = render.render(Templates.BEAN_DTO, contextElements);
            this.writeFile(bean.getEdoBuilder(), pojoPath, javaContent);

            //service interface
            String serviceInterfaceFilePath = ServiceFileBuilder.getServiceInterfaceFilePath(bean);
            String serviceInterfaceJava = render.render(Templates.SERVICE_INTERFACE_JAVA, contextElements);
            this.writeFile(bean.getEdoBuilder(), serviceInterfaceFilePath, serviceInterfaceJava);

            //service 
            String serviceFilePath = ServiceFileBuilder.getServiceFilePath(bean);
            String serviceJava = render.render(Templates.SERVICE_JAVA, contextElements);
            this.writeFile(bean.getEdoBuilder(), serviceFilePath, serviceJava);

        } catch (Throwable t) {
            logger.error("error writing files for {}", bean.getName(), t);
        }
    }


    private void writeStruts2Action(Render render, Map<String, Object> contextElements, EdoBean bean) {
        try {

            //ACTION
            String actionFilePath = ControllerFileBuilder.getActionFilePath(bean);
            String actionJava = render.render(Templates.ACTION_JAVA, contextElements);
            this.writeFile(bean.getEdoBuilder(), actionFilePath, actionJava);

            //FINDER_ACTION
            String finderActionFilePath = ControllerFileBuilder.getFinderActionFilePath(bean);
            String finderActionJava = render.render(Templates.ACTION_FINDER_JAVA, contextElements);
            this.writeFile(bean.getEdoBuilder(), finderActionFilePath, finderActionJava);

            //ACTION_XML
            String actionXmlFilePath = ControllerFileBuilder.getActionXmlFilePath(bean);
            String actionXmlContent = render.render(Templates.ACTION_XML, contextElements);
            this.writeFile(bean.getEdoBuilder(), actionXmlFilePath, actionXmlContent);

            //CONVERSION_PROPERTIES
            if (bean.needsActionStrutsConversionProperties()) {
                String propFilePath = ControllerFileBuilder.getActionConversionFilePath(bean);
                String actionConvPropertiesContent = render.render(Templates.ACTION_CONVERSION_XML, contextElements);
                this.writeFile(bean.getEdoBuilder(), propFilePath, actionConvPropertiesContent);
            }
            if (bean.needsFinderActionStrutsConversionProperties()) {
                String convFilePath = ControllerFileBuilder.getActionFinderConversionFilePath(bean);
                String actionConvPropertiesContent = render.render(Templates.ACTION_FINDER_CONVERSION_XML, contextElements);
                this.writeFile(bean.getEdoBuilder(), convFilePath, actionConvPropertiesContent);
            }

            //VALIDATION
            if (bean.needsStrutsActionValidation()) {
                String validationFilePath = ControllerFileBuilder.getActionValidationXmlFilePath(bean);
                String actionValidationContent = render.render(Templates.ACTION_VALIDATION_XML, contextElements);
                this.writeFile(bean.getEdoBuilder(), validationFilePath, actionValidationContent);
            }

            //PACKAGE.PROPERTIES
            String actionPropertiesItFilePath = ControllerFileBuilder.getActionPropertiesFilePath(bean, "it");
            String actionPropertiesItContent = render.render(Templates.ACTION_PACKAGE_PROPERTIES, contextElements);
            this.writeFile(bean.getEdoBuilder(), actionPropertiesItFilePath, actionPropertiesItContent);
            String actionPropertiesEnFilePath = ControllerFileBuilder.getActionPropertiesFilePath(bean, "en");
            String actionPropertiesEnContent = render.render(Templates.ACTION_PACKAGE_PROPERTIES, contextElements);
            this.writeFile(bean.getEdoBuilder(), actionPropertiesEnFilePath, actionPropertiesEnContent);



            //SHORTCUTS
            String shortcutFilePath = ControllerFileBuilder.getActionShortCutFilePath(bean);
            String shortcutContent = render.render(Templates.SHORTCUT, contextElements);
            this.writeFile(bean.getEdoBuilder(), shortcutFilePath, shortcutContent);

            //TILES
            String tilesFilePath = ControllerFileBuilder.getActionTilesFilePath(bean);
            String actionTilesContent = render.render(Templates.ACTION_TILES, contextElements);
            this.writeFile(bean.getEdoBuilder(), tilesFilePath, actionTilesContent);



            if (bean.getEdoBuilder().isPlugin()) {
                //GLOBALMESSAGES.PROPERTIES
                String globalMessagesPropertiesFilePath = ControllerFileBuilder.getGlobalMessagesPropertiesFilePath(bean, "it");
                String globalMessagesPropertiesContent = render.render(Templates.ACTION_PLUGIN_GLOBAL_MESSAGES_PROPERTIES, contextElements);
                this.writeFile(bean.getEdoBuilder(), globalMessagesPropertiesFilePath, globalMessagesPropertiesContent);

                //GLOBALMESSAGES.PROPERTIES
                globalMessagesPropertiesFilePath = ControllerFileBuilder.getGlobalMessagesPropertiesFilePath(bean, "en");
                globalMessagesPropertiesContent = render.render(Templates.ACTION_PLUGIN_GLOBAL_MESSAGES_PROPERTIES, contextElements);
                this.writeFile(bean.getEdoBuilder(), globalMessagesPropertiesFilePath, globalMessagesPropertiesContent);
            }


        } catch (Throwable t) {
            logger.error("error writing files for {}", bean.getName(), t);
        }
    }


    private void writeSpecilWidget(Render render, Map<String, Object> contextElements, EdoBean bean) {
        try {
            //SPECIAL WIDGET_ACTION
            String specialWidgetActionFilePath = WidgetFileBuilder.getSpecialWidgetActionFilePath(bean);
            String specialWidgetActionJava = render.render(Templates.ACTION_SPECIALWIDGET_JAVA, contextElements);
            this.writeFile(bean.getEdoBuilder(), specialWidgetActionFilePath, specialWidgetActionJava);

            //SPECIAL WIDGET_ACTION_XML
            String specialWidgetActionXmlFilePath = WidgetFileBuilder.getSpecialWidgetActionXmlFilePath(bean);
            String specialWidgetActionXmlContent = render.render(Templates.SPECIALWIDGET_ACTION_XML, contextElements);
            this.writeFile(bean.getEdoBuilder(), specialWidgetActionXmlFilePath, specialWidgetActionXmlContent);

            //PACKAGE.PROPERTIES
            String specialWidgetActionPropertiesItFilePath = WidgetFileBuilder.getSpecialWidgetActionPropertiesFilePath(bean, "it");
            String specialWidgetActionPropertiesItContent = render.render(Templates.ACTION_SPECIALWIDGET_PACKAGE_PROPERTIES, contextElements);
            this.writeFile(bean.getEdoBuilder(), specialWidgetActionPropertiesItFilePath, specialWidgetActionPropertiesItContent);

            String specialWidgetActionPropertiesEnFilePath = WidgetFileBuilder.getSpecialWidgetActionPropertiesFilePath(bean, "en");
            String specialWidgetActionPropertiesEnContent = render.render(Templates.ACTION_SPECIALWIDGET_PACKAGE_PROPERTIES, contextElements);
            this.writeFile(bean.getEdoBuilder(), specialWidgetActionPropertiesEnFilePath, specialWidgetActionPropertiesEnContent);

            //SPECIAL WIDGET_JSP
            String specialWidgetJspFilePath = WidgetFileBuilder.getJspSpecialWidgetFilePath(bean);
            String specialWidgetJsp = render.render(Templates.SPECIALWIDGET_JSP, contextElements);
            this.writeFile(bean.getEdoBuilder(), specialWidgetJspFilePath, specialWidgetJsp);

            //TAGS
            String listTagFilePath = WidgetFileBuilder.getTagListFilePath(bean);
            String listTagJava = render.render(Templates.TAG_LIST_JAVA, contextElements);
            this.writeFile(bean.getEdoBuilder(), listTagFilePath, listTagJava);

            String singleTagFilePath = WidgetFileBuilder.getTagSingleFilePath(bean);
            String singleTagJava = render.render(Templates.TAG_SINGLE_JAVA, contextElements);
            this.writeFile(bean.getEdoBuilder(), singleTagFilePath, singleTagJava);

            //TLD
            String apsTldFilePath = WidgetFileBuilder.getApsTldFilePath(bean);
            String apsTld = render.render(Templates.TLD_APS, contextElements);
            this.writeFile(bean.getEdoBuilder(), apsTldFilePath, apsTld);

            //WIDGET_JSP
            String widgetJspFilePath = WidgetFileBuilder.getJspWidgetFilePath(bean);
            String widgetJsp = render.render(Templates.WIDGET_JSP, contextElements);
            this.writeFile(bean.getEdoBuilder(), widgetJspFilePath, widgetJsp);

        } catch (Throwable t) {
            logger.error("error writing files for {}", bean.getName(), t);
        }
    }

    private void writeInternalServlet(Render render, Map<String, Object> contextElements, EdoBean bean) {
        //INTERNAL_SERVLET_XML
        try {
            String internalServletXmlFilePath = InternalServletFileBuilder.getInternalServletXmlFilePath(bean);
            String internalServletXmlContent = render.render(Templates.INTERNAL_SERVLET_XML_ACTION, contextElements);
            this.writeFile(bean.getEdoBuilder(), internalServletXmlFilePath, internalServletXmlContent);

            String internalServletActionFilePath = InternalServletFileBuilder.getInternalServletActionFilePath(bean);
            String internalServletActionContent = render.render(Templates.INTERNAL_SERVLET_ACTION, contextElements);
            this.writeFile(bean.getEdoBuilder(), internalServletActionFilePath, internalServletActionContent);

            String internalServletFinderActionFilePath = InternalServletFileBuilder.getInternalServletFinderActionFilePath(bean);
            String internalServletFinderActionContent = render.render(Templates.INTERNAL_SERVLET_ACTION_FINDER, contextElements);
            this.writeFile(bean.getEdoBuilder(), internalServletFinderActionFilePath, internalServletFinderActionContent);

            String internalServletJspListFilePath = InternalServletFileBuilder.getInternalServletJspListFilePath(bean);
            String internalServletJspListFileContent = render.render(Templates.INTERNAL_SERVLET_JSP_LIST, contextElements);
            this.writeFile(bean.getEdoBuilder(), internalServletJspListFilePath, internalServletJspListFileContent);

            String internalServletJspEntryFilePath = InternalServletFileBuilder.getInternalServletJspEntryFilePath(bean);
            String internalServletJspEntryFileContent = render.render(Templates.INTERNAL_SERVLET_JSP_ENTRY, contextElements);
            this.writeFile(bean.getEdoBuilder(), internalServletJspEntryFilePath, internalServletJspEntryFileContent);

            String internalServletJspTrashFilePath = InternalServletFileBuilder.getInternalServletJspTrashFilePath(bean);
            String internalServletJspTrashFileContent = render.render(Templates.INTERNAL_SERVLET_JSP_TRASH, contextElements);
            this.writeFile(bean.getEdoBuilder(), internalServletJspTrashFilePath, internalServletJspTrashFileContent);

            String internalServletJspErrorFilePath = InternalServletFileBuilder.getInternalServletJspErrorFilePath(bean);
            String internalServletJspErrorFileContent = render.render(Templates.INTERNAL_SERVLET_JSP_ERROR, contextElements);
            this.writeFile(bean.getEdoBuilder(), internalServletJspErrorFilePath, internalServletJspErrorFileContent);
        } catch (Throwable t) {
            logger.error("error writing files for {}", bean.getName(), t);
        }
    }


    private void writeStruts2Jsp(Render render, Map<String, Object> contextElements, EdoBean bean) {
        try {
            //ENTRY
            String entryFilePath = ControllerFileBuilder.getJspEntryFilePath(bean);
            String actionJsp = render.render(Templates.JSP_APSADMIN_ENTRY, contextElements);
            this.writeFile(bean.getEdoBuilder(), entryFilePath, actionJsp);

            String entryExtrasFilePath = ControllerFileBuilder.getJspExtraResourcesEntryFilePath(bean);
            String entryExtrasJsp = render.render(Templates.JSP_APSADMIN_ENTRY_EXTRA_RESOURCES, contextElements);
            this.writeFile(bean.getEdoBuilder(), entryExtrasFilePath, entryExtrasJsp);

            //LIST
            String listFilePath = ControllerFileBuilder.getJspListFilePath(bean);
            String actionListJsp = render.render(Templates.JSP_APSADMIN_LIST, contextElements);
            this.writeFile(bean.getEdoBuilder(), listFilePath, actionListJsp);

            String listExtrasFilePath = ControllerFileBuilder.getJspExtraResourcesListFilePath(bean);
            String listExtrasJsp = render.render(Templates.JSP_APSADMIN_LIST_EXTRA_RESOURCES, contextElements);
            this.writeFile(bean.getEdoBuilder(), listExtrasFilePath, listExtrasJsp);


            //TRASH
            String trashFilePath = ControllerFileBuilder.getJspTrashFilePath(bean);
            String actionTrashJsp = render.render(Templates.JSP_APSADMIN_TRASH, contextElements);
            this.writeFile(bean.getEdoBuilder(), trashFilePath, actionTrashJsp);
            String trashExtrasFilePath = ControllerFileBuilder.getJspExtraResourcesTrashFilePath(bean);
            String trashExtrasJsp = render.render(Templates.JSP_APSADMIN_TRASH_EXTRA_RESOURCES, contextElements);
            this.writeFile(bean.getEdoBuilder(), trashExtrasFilePath, trashExtrasJsp);

            if (bean.getEdoBuilder().isPlugin()) {
                //jsp submenu
                String submenuFilePath = ControllerFileBuilder.getJspSubMenuFilePath(bean);
                String submenuJsp = render.render(Templates.JSP_APSADMIN_PLUGINSUBMENU, contextElements);
                this.writeFile(bean.getEdoBuilder(), submenuFilePath, submenuJsp);
            }


        } catch (Throwable t) {
            logger.error("error writing files for {}", bean.getName(), t);
        }
    }

    private void writeTestService(Render render, Map<String, Object> contextElements, EdoBean bean) {
        try {
            //ConfigTestUtil
            String configTestUtil = TestCaseFileBuilder.getTestConfigTestUtilFilePath(bean);
            String configTestUtilJava = render.render(Templates.TEST_CONFIG_TEST_UTILS, contextElements);
            this.writeFile(bean.getEdoBuilder(), configTestUtil, configTestUtilJava);

            //BASE TEST
            String baseTestFilePath = TestCaseFileBuilder.getTestApsBaseTestCaseFilePath(bean);
            String baseTestJava = render.render(Templates.TEST_SERVICES_BASE_TEST_JAVA, contextElements);
            this.writeFile(bean.getEdoBuilder(), baseTestFilePath, baseTestJava);

            //Manager TEST
            String managerTestFilePath = TestCaseFileBuilder.getTestManagerFilePath(bean);
            String managerTestJava = render.render(Templates.TEST_MANAGER_JAVA, contextElements);
            this.writeFile(bean.getEdoBuilder(), managerTestFilePath, managerTestJava);

        } catch (Throwable t) {
            logger.error("error!", t);
        }
    }

    private void writeTestAction(Render render, Map<String, Object> contextElements, EdoBean bean) {
        try {
            //BASE TEST
            String baseTestactionFilePath = TestCaseFileBuilder.getTestApsAdminBaseActionFilePath(bean);
            String baseTestActionJava = render.render(Templates.TEST_ACTION_BASE_TEST_JAVA, contextElements);
            this.writeFile(bean.getEdoBuilder(), baseTestactionFilePath, baseTestActionJava);

            //ACTION
            String actionFilePath = TestCaseFileBuilder.getTestActionFilePath(bean);
            String actionJava = render.render(Templates.TEST_ACTION_JAVA, contextElements);
            this.writeFile(bean.getEdoBuilder(), actionFilePath, actionJava);

            //FINDER_ACTION
            String finderActionFilePath = TestCaseFileBuilder.getTestFinderActionFilePath(bean);
            String finderActionJava = render.render(Templates.TEST_ACTION_FINDER_JAVA, contextElements);
            this.writeFile(bean.getEdoBuilder(), finderActionFilePath, finderActionJava);

        } catch (Throwable t) {
            logger.error("error writing files for {}", bean.getName(), t);
        }
    }


    protected void writeFile(EdoBuilder builder, String path, String text) {
        try {
            Report report = Report.getInstance();
            EdoReportEntry reportEntry = builder.getEdoWriter().write(path, text);
            String workingDir = builder.getBaseDir();
            int lines = reportEntry.getLines();
            path = StringUtils.substringAfter(path, workingDir + File.separator);
            if (FilenameUtils.getBaseName(path).startsWith(EdoConstants.FILE_NO_OVERRIDE_PREFIX)) {
                report.addFileToMerge(path, lines);
            } else {
                report.addFile(path, lines);
            }

        } catch (Throwable t) {
            logger.error("error writing file {}", path, t);
        }
    }

}
