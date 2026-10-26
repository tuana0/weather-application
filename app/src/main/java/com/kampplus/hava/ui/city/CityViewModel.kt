package com.kampplus.hava.ui.city

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonSyntaxException
import com.kampplus.hava.data.CityRepository
import com.kampplus.hava.data.OpenMeteoCityRepository
import com.kampplus.hava.model.City
import com.kampplus.hava.ui.LoadState
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * Şehir listesi ekranının beyni.
 *
 * Görev dağılımı:
 * - Ekran (Composable) yalnızca çizer.
 * - ViewModel state'i tutar ve kararları verir.
 * - Repository veriyi getirir.
 *
 * ViewModel ekran döndüğünde (Activity yeniden kurulurken) yaşamaya devam eder;
 * bu yüzden arama metni ve sonuçlar kaybolmaz.
 *
 * @param repository Varsayılanı gerçek servis. Testlerde sahte bir repository verilir.
 *   Tüm parametrelerin varsayılanı olduğu için Kotlin parametresiz bir kurucu da üretir;
 *   ekranlarda `viewModel()` bunu kullanabilir.
 */
class CityViewModel(
    private val repository: CityRepository = OpenMeteoCityRepository()
) : ViewModel() {

    // State ViewModel'de MutableStateFlow ile tutulur.
    // Değiştirilebilir alan private'tır; dışarıya yalnızca okunabilir hâli açılır.
    private val _query = MutableStateFlow(INITIAL_QUERY)
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<LoadState<List<City>>>(LoadState.Loading)
    val uiState: StateFlow<LoadState<List<City>>> = _uiState.asStateFlow()

    init {
        // Ekran boş açılmasın diye açılışta bir arama yapılır.
        search()
    }

    /** Kullanıcı arama kutusuna her harf yazdığında çağrılır. */
    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        search()
    }

    /**
     * Aramayı çalıştırır ve sonucu dört durumdan birine çevirir.
     *
     * `viewModelScope.launch`: yeni bir coroutine başlatır. Ekran kapanırsa bu kapsam
     * işi otomatik iptal eder, yarıda kalan istek sızıntı yapmaz.
     */
    fun search() {
        viewModelScope.launch {
            if (_query.value.isBlank()) {
                _uiState.value = LoadState.Success(STATIC_CITIES)
                return@launch
            }

            // 1. Önce 10 sabit şehir içerisinde arama süzmesi yapalım:
            val filteredStatic = STATIC_CITIES.filter {
                it.name.contains(_query.value, ignoreCase = true)
            }

            if (filteredStatic.isNotEmpty()) {
                _uiState.value = LoadState.Success(filteredStatic)
                return@launch
            }

            // 2. Sabit listede eşleşme yoksa internet (API) üzerinden arayalım:
            _uiState.value = LoadState.Loading
            _uiState.value = try {
                val cities = repository.search(_query.value)
                if (cities.isEmpty()) LoadState.Empty else LoadState.Success(cities)
            } catch (e: IOException) {
                Log.e(TAG, "Arama başarısız: bağlantı hatası", e)
                LoadState.Empty
            } catch (e: HttpException) {
                Log.e(TAG, "Arama başarısız: HTTP ${e.code()}", e)
                LoadState.Empty
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, "Arama başarısız: yanıt çözümlenemedi", e)
                LoadState.Empty
            }
        }
    }

    private companion object {
        const val TAG = "CityViewModel"

        /** Uygulama ilk açıldığında aranan şehir. */
        const val INITIAL_QUERY = ""

        val STATIC_CITIES = listOf(
            City(1, "Ankara", 39.91987, 32.85427, "TR", "Türkiye", "Ankara"),
            City(2, "İstanbul", 41.0050, 28.9766, "TR", "Türkiye", "İstanbul"),
            City(3, "İzmir", 38.41985, 27.1277, "TR", "Türkiye", "İzmir"),
            City(4, "Bursa", 40.2901, 29.0489, "TR", "Türkiye", "Bursa"),
            City(5, "Adana", 37.0000, 35.0000, "TR", "Türkiye", "Adana"),
            City(6, "Antalya", 36.8970, 30.7042, "TR", "Türkiye", "Antalya"),
            City(7, "Aydın", 37.1333, 28.4167, "TR", "Türkiye", "Aydın"),
            City(8, "Balıkesir", 39.6167, 27.9167, "TR", "Türkiye", "Balıkesir"),
            City(9, "Çanakkale", 40.1667, 36.0500, "TR", "Türkiye", "Çanakkale"),
            City(10, "Çorum", 41.1167, 36.8167, "TR", "Türkiye", "Çorum")

        )

    }
}
