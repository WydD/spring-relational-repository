package fr.petitl.relational.repository.support;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.w3c.dom.Element;

/**
 *
 */
public class RelationalRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {
    @Override
    protected String getModulePrefix() {
        return "jdbc";
    }

    @Override
    public String getRepositoryFactoryClassName() {
        return RelationalRepositoryFactoryBean.class.getName();
    }

    @Override
    public void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config) {
        AnnotationAttributes attributes = config.getAttributes();
        builder.addPropertyReference("operations", attributes.getString("relationalTemplateRef"));
    }

    @Override
    public void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config) {
        Element element = config.getElement();
        builder.addPropertyReference("operations", element.getAttribute("relational-template-ref"));
    }
}
