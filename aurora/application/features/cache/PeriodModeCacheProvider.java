package aurora.application.features.cache;

import java.util.Timer;
import java.util.TimerTask;

import aurora.bm.ICachedDataProvider;
import aurora.bm.IModelFactory;

import uncertain.cache.INamedCacheFactory;
import uncertain.exception.BuiltinExceptionFactory;
import uncertain.ocm.IObjectRegistry;

public class PeriodModeCacheProvider extends CacheProvider {

	protected int refreshInterval = 3600000;//1 hour

	public PeriodModeCacheProvider(IObjectRegistry registry, INamedCacheFactory cacheFactory,ICachedDataProvider cacheDataProvider,IModelFactory modelFactory) {
		super(registry, cacheFactory,cacheDataProvider,modelFactory);
	}

	public void initialize() {
		if (refreshInterval == -1)
			throw BuiltinExceptionFactory.createAttributeMissing(this, "refreshInterval");
		super.initialize();
	}

	protected void initReloadTimer() {
		reloadTimer = new Timer(getCacheName()+"_reload_timer");
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				while (!shutdown) {
					synchronized (reloadLock) {
						try {
							reloadLock.wait(refreshInterval);
						} catch (InterruptedException e) {
						}
						if (!shutdown)
							reloadWithTrx();
					}
				}
			}
		};
		reloadTimer.schedule(timerTask, 0);
	}

	public int getRefreshInterval() {
		return refreshInterval;
	}

	public void setRefreshInterval(int refreshInterval) {
		this.refreshInterval = refreshInterval;
	}
}
