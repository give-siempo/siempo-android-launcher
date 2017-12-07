package co.siempo.phone.mm;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;

import co.siempo.phone.R;
import co.siempo.phone.app.Launcher3App;
import co.siempo.phone.util.PackageUtil;
import de.greenrobot.event.Subscribe;
import minium.co.core.app.CoreApplication;
import minium.co.core.event.AppInstalledEvent;
import minium.co.core.ui.CoreActivity;

@EActivity(R.layout.time_picker_custom)

public class MMTimePickerActivity extends CoreActivity {

    @Subscribe
    public void appInstalledEvent(AppInstalledEvent event) {
        if (event.isRunning()) {
            ((Launcher3App) CoreApplication.getInstance()).setAllDefaultMenusApplication();
        }
    }
    @AfterViews
    public void afterViews() {
        loadFragment(new MMTimePickerFragment_(), R.id.mainView, "Main");
    }

    @Override
    protected void onResume() {
        super.onResume();
        PackageUtil.checkPermission(this);
    }
}
