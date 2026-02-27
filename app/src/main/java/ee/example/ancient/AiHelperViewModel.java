package ee.example.ancient;
//此文件的目标是完成五号ai创作的缓存需求
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AiHelperViewModel extends ViewModel {
    private final MutableLiveData<String> resultText = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public LiveData<String> getResultText() {
        return resultText;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void setResultText(String text) {
        resultText.setValue(text);
    }

    public void setLoading(boolean loading) {
        isLoading.setValue(loading);
    }
}