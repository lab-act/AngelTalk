package act.sds.samsung.angelman.dagger.modules;

import android.content.Context;

import javax.inject.Singleton;

import act.sds.samsung.angelman.data.repository.CardDataRepository;
import act.sds.samsung.angelman.data.repository.CategoryDataRepository;
import act.sds.samsung.angelman.data.transfer.CardTransfer;
import act.sds.samsung.angelman.domain.repository.CardRepository;
import act.sds.samsung.angelman.domain.repository.CategoryRepository;
import act.sds.samsung.angelman.presentation.util.ApplicationManager;
import dagger.Module;
import dagger.Provides;

@Module
public class AngelmanModule {

    Context context;
    public AngelmanModule(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    CardRepository providesSingleCardRepository() {
        return new CardDataRepository(context.getApplicationContext());
    }

    @Provides
    @Singleton
    CategoryRepository providesCategoryRepository() {
        return new CategoryDataRepository(context.getApplicationContext());
    }

    @Provides
    @Singleton
    CardTransfer providesCardTransfer() {
        return  new CardTransfer();
    }

    @Provides
    @Singleton
    ApplicationManager providesApplicationManager() {
        return  new ApplicationManager(context.getApplicationContext());
    }
}
