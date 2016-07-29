package pl.epodr.sk.common.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
class AutowireManagerImpl implements AutowireManager {

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void autowireFields(Object object) {
		CustomAutowiring.autowireFields(object, applicationContext);
	}

}
