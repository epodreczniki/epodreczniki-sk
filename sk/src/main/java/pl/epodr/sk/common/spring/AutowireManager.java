package pl.epodr.sk.common.spring;

import org.springframework.context.ApplicationContextAware;

public interface AutowireManager extends ApplicationContextAware {

	void autowireFields(Object object);

}
