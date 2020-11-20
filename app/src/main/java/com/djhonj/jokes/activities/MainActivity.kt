package com.djhonj.jokes.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.djhonj.jokes.BuildConfig
import com.djhonj.jokes.R
import com.djhonj.jokes.models.Joke
import com.djhonj.jokes.models.Translate
import com.djhonj.jokes.models.Translations
import com.djhonj.jokes.services.JokeService
import com.djhonj.jokes.services.ServiceBuilder
import com.djhonj.jokes.services.TranslatorService
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.Serializable

class MainActivity : AppCompatActivity() {
    private var jokeSaveInstance: List<Joke>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val jokes = savedInstanceState?.getSerializable("jokeInstance")

        if (jokes == null) {
            progressBar.visibility = View.VISIBLE
            loadJokeRandom()
        }

        buttonBuscar.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            loadJokeRandom()
        }

        buttonTraducir.setOnClickListener {
            val setup: String = textViewSetup.text.toString().split("-")[1]
            val punchline: String = textViewPunchLine.text.toString().split("-")[1]

            progressBar.visibility = View.VISIBLE

            traducir(Translate(listOf(setup,  punchline), "en-es"))
        }
    }

    //guardamos el valor en la saveinstance
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (jokeSaveInstance != null) {
            outState.putSerializable("jokeInstance", jokeSaveInstance as Serializable)
        }
    }

    //obtenemos el valor
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState.getSerializable("jokeInstance") != null) {
            val jokes: List<Joke>? = savedInstanceState.getSerializable("jokeInstance") as List<Joke>
            jokes?.get(0)?.let {
                textViewSetup.text = "- ${it.setup}"
                textViewPunchLine.text = "- ${it.punchline}"

                jokeSaveInstance = listOf(it)
            }
        }
    }

    private fun loadJokeRandom() {
        val jokeService: JokeService = ServiceBuilder.buildService(JokeService::class.java)
        val requestGet: Call<Joke> = jokeService.getJokeRandom()

        requestGet.enqueue(object: Callback<Joke> {
            override fun onFailure(call: Call<Joke>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Problemas en la peticion onFailure", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<Joke>, response: Response<Joke>) {
                if (response.isSuccessful) {
                    linear_layout_traduccion.visibility = View.INVISIBLE

                    val joke: Joke? = response.body()
                    joke?.let {
                        progressBar.visibility = View.INVISIBLE

                        textViewSetup.text = "- ${it.setup}"
                        textViewPunchLine.text = "- ${it.punchline}"
                        tv_type.text = "type: ${it.type}"

                        jokeSaveInstance = listOf(it)
                    }
                }
            }
        })
    }

    private fun traducir(translate: Translate): Joke? {
        val serviceBuilder = ServiceBuilder.changeUrlBase(BuildConfig.API_JOKES_URL, BuildConfig.API_TRANSLATOR_URL)
        val translatorService: TranslatorService = serviceBuilder.buildService(TranslatorService::class.java, BuildConfig.API_TRANSLATOR_USERNAME, BuildConfig.API_TRANSLATOR_PASSWORD)
        val request: Call<Translations> = translatorService.translator(translate)
        val joke: Joke? = null

        request.enqueue(object: Callback<Translations> {
            override fun onFailure(call: Call<Translations>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Problemas al traducir", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<Translations>, response: Response<Translations>) {
                if (response.isSuccessful) {
                    val translation: Translations? = response.body()
                    translation?.let {
                        var setup = it.translations.get(0).get("translation").toString()
                        var punch = it.translations.get(1).get("translation").toString()

                        //joke?.setup = setup
                        //joke?.punchline = punch

                        tv_setup_spanish.text = "-${setup}"
                        tv_punchline_spanish.text = "-${punch}"

                        linear_layout_traduccion.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Problemas al traducir", Toast.LENGTH_SHORT).show()
                }
            }
        })

        return joke
    }
}
