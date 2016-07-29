package pl.epodr.sk.common.spring;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;

final class CustomAutowiring {

	private static final Logger logger = LoggerFactory.getLogger(CustomAutowiring.class);

	public static void autowireFields(Object object, ApplicationContext context) {
		Class<?> clazz = object.getClass();
		logger.trace("autowiring fields in class " + clazz);
		List<Field> fields = getAllFields(clazz);
		for (Field field : fields) {
			Autowired annotation = field.getAnnotation(Autowired.class);
			if (annotation != null) {
				try {
					autowireField(object, context, field, annotation);
				} catch (IllegalArgumentException e) {
					logger.error("when autowiring field " + field, e);
				} catch (IllegalAccessException e) {
					logger.error("when autowiring field " + field, e);
				}
			}
		}
	}

	private static void autowireField(Object object, ApplicationContext context, Field field, Autowired annotation)
			throws IllegalAccessException {
		logger.trace("autowiring field " + field);
		field.setAccessible(true);
		Class<?> type = field.getType();
		if (annotation.required()) {
			Object bean;
			if (type != TaskExecutor.class) {
				bean = context.getBean(type);
			} else {
				bean = context.getBean("taskExecutor");
			}
			field.set(object, bean);
		} else {
			try {
				field.set(object, context.getBean(type));
			} catch (NoSuchBeanDefinitionException e) {
				field.set(object, Mockito.mock(type));
			}
		}
	}

	private static List<Field> getAllFields(Class<?> clazz) {
		List<Field> fields = new ArrayList<Field>();
		fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
		if (clazz.getSuperclass() != null) {
			fields.addAll(getAllFields(clazz.getSuperclass()));
		}
		return fields;
	}

	private CustomAutowiring() {
	}

}
