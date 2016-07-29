package pl.epodr.sk.common.spring;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAsync
@EnableScheduling
public class SpringConfiguration implements AsyncConfigurer {

	@Override
	public Executor getAsyncExecutor() {
		return new HandlingExecutor();
	}
}

@Slf4j
class HandlingExecutor extends SimpleAsyncTaskExecutor {

	public HandlingExecutor() {
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread t, Throwable e) {
				HandlingExecutor.this.handle(e);
			}
		});
	}

	@Override
	public void execute(Runnable task) {
		super.execute(createWrappedRunnable(task));
	}

	@Override
	public void execute(Runnable task, long startTimeout) {
		super.execute(createWrappedRunnable(task), startTimeout);
	}

	@Override
	public Future<?> submit(Runnable task) {
		return super.submit(createWrappedRunnable(task));
	}

	@Override
	public <T> Future<T> submit(final Callable<T> task) {
		return super.submit(createCallable(task));
	}

	private <T> Callable<T> createCallable(final Callable<T> task) {
		return new Callable<T>() {

			@Override
			public T call() throws Exception {
				try {
					return task.call();
				} catch (Exception e) {
					handle(e);
					throw e;
				}
			}
		};
	}

	private Runnable createWrappedRunnable(final Runnable task) {
		return new Runnable() {

			@Override
			public void run() {
				try {
					task.run();
				} catch (Exception e) {
					handle(e);
				}
			}
		};
	}

	private void handle(Throwable e) {
		log.error("uncaught exception", e);
	}
}