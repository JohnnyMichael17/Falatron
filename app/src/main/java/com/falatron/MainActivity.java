package com.falatron;

import static androidx.constraintlayout.widget.ConstraintLayoutStates.TAG;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;

import android.animation.ValueAnimator;

import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import android.Manifest;

public class MainActivity extends AppCompatActivity {

    private List<String> categoryList;
    private List<Models> modelsList;
    private String selectedName;
    private String selectedCategory;

    private Button gerarAudio_button;
    private Button playPause_button;
    private boolean isPlaying = false;
    private boolean isMuted = false;
    private SeekBar seekBar;
    private Button muteButton;
    private MediaPlayer mediaPlayer;
    private String base64Audio;

    private ValueAnimator progressAnimator;
    private int currentProgress;
    private TextView queueAux;

    private EditText text_box;
    int contadorDeAudios = 0;

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int NOTIFICATION_ID = 1;

    private Button buttonOk;
    private TextView permissionText;
    private int contador = 10;
    private SharedPreferences sharedPreferences;

    private ProgressBar loadingProgressBar;
    private Handler handler = new Handler();

    private AdView adView1;
    private AdView adView2;

    private JSONObject jsonObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //------ Colocando anúncio/AD ------//
        adView1 = new AdView(this);
        adView1.setAdSize(AdSize.BANNER);
        adView1.setAdUnitId("ca-app-pub-7015560586203687/3126600134");

        adView2 = new AdView(this);
        adView2.setAdSize(AdSize.BANNER);
        adView2.setAdUnitId("ca-app-pub-7015560586203687/9237600961");

        // Encontre o layout onde você deseja adicionar o anúncio
        ViewGroup anuncio01 = findViewById(R.id.anuncio01);
        ViewGroup anuncio02 = findViewById(R.id.anuncio02);

        // Adicione o AdView ao layout
        anuncio01.addView(adView1);
        anuncio02.addView(adView2);

        // Carregue o anúncio
        AdRequest adRequest = new AdRequest.Builder().build();
        adView1.loadAd(adRequest);

        AdRequest adRequest2 = new AdRequest.Builder().build();
        adView2.loadAd(adRequest2);

        //------ Implementando os Spinners para o usuário selecionar uma Categoria e uma Voz ------//
        Spinner spinnerCategoria = (Spinner) findViewById(R.id.spinner_categoria);
        Spinner spinnerVoz = (Spinner) findViewById(R.id.spinner_voz);

        CardView cardView = (CardView) findViewById(R.id.cardView);

        modelsList = new ArrayList<>();
        categoryList = new ArrayList<>();

        loadingProgressBar = findViewById(R.id.loadingProgressBar);

        try {

            if (testInternet()) {
                jsonObject = new JSONObject(downloadJson("https://falatron.com/static/models.json"));
                updateJsonInAssets(this, "models", jsonObject);
            } else {
                jsonObject = new JSONObject(loadJSONFromAsset());
            }

            JSONArray modelsArray = jsonObject.getJSONArray("models");

            List<String> categoriesList = new ArrayList<>();
            List<String> namesList = new ArrayList<>();

            categoriesList.add("Selecione a categoria...");
            categoriesList.add("Todas as vozes");

            // Itera sobre os modelos para extrair as categorias e nomes
            for (int i = 0; i < modelsArray.length(); i++) {
                JSONObject modelObject = modelsArray.getJSONObject(i);
                String category = modelObject.getString("category");
                String name = modelObject.getString("name");

                // Adiciona a categoria à lista se ainda não estiver presente
                if (!categoriesList.contains(category)) {
                    categoriesList.add(category);
                }

                // Adiciona o nome à lista
                namesList.add(name);
            }

            Collections.sort(categoriesList.subList(2, categoriesList.size()));

            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, R.layout.spinner_categoria_text, categoriesList);
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategoria.setAdapter(categoryAdapter);

            spinnerCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // Obtém a categoria selecionada
                    selectedCategory = categoriesList.get(position);

                    // Cria uma lista para armazenar os nomes da categoria selecionada
                    List<String> filteredNamesList = new ArrayList<>();

                    filteredNamesList.add("Selecione a voz...");

                    // Itera sobre os modelos para encontrar os nomes da categoria selecionada
                    if (selectedCategory != "Todas as vozes") {
                        // Mostra as vozes da Categoria selecionada

                        for (int i = 0; i < modelsArray.length(); i++) {
                            try {
                                JSONObject modelObject = modelsArray.getJSONObject(i);
                                String category = modelObject.getString("category");
                                String name = modelObject.getString("name");

                                // Verifica se a categoria do modelo corresponde à categoria selecionada
                                if (category.equals(selectedCategory)) {
                                    filteredNamesList.add(name);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                    } else {
                        // Mostra todas as vozes

                        for (int i = 0; i < modelsArray.length(); i++) {
                            try {
                                JSONObject modelObject = modelsArray.getJSONObject(i);
                                String name = modelObject.getString("name");

                                filteredNamesList.add(name);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    Collections.sort(filteredNamesList.subList(1, filteredNamesList.size()));

                    ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.spinner_voz_text, filteredNamesList);
                    nameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerVoz.setAdapter(nameAdapter);

                    ImageView imageModel = findViewById(R.id.image);
                    TextView nameModel = findViewById(R.id.nameModel);
                    TextView authorModel = findViewById(R.id.authorModel);
                    TextView dubladorModel = findViewById(R.id.dubladorModel);

                    spinnerVoz.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                            selectedName = (String) parent.getItemAtPosition(position);

                            JSONObject selectedModel = null;

                            for (int i = 0; i < modelsArray.length(); i++) {
                                try {
                                    JSONObject modelObject = modelsArray.getJSONObject(i);
                                    String name = modelObject.getString("name");
                                    if (name.equals(selectedName)) {
                                        selectedModel = modelObject;
                                        break;
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            // Atualiza o CardView com os dados do usuário selecionado
                            if (selectedModel != null) {

                                String image = selectedModel.optString("image");
                                String author = selectedModel.optString("author");
                                String dublador = selectedModel.optString("dublador");

                                nameModel.setText(selectedName);
                                authorModel.setText(author);

                                if (!dublador.isEmpty()) {
                                    dubladorModel.setText(dublador);
                                    findViewById(R.id.dublador).setVisibility(View.VISIBLE);
                                    findViewById(R.id.dubladorModel).setVisibility(View.VISIBLE);
                                } else {
                                    findViewById(R.id.dublador).setVisibility(View.GONE);
                                    findViewById(R.id.dubladorModel).setVisibility(View.GONE);
                                }

                                cardView.setVisibility(View.VISIBLE);

                                // Carrega a imagem usando uma biblioteca de carregamento de imagem (Glide)

                                if (image.isEmpty()) {
                                    findViewById(R.id.img_ic_logo).setVisibility(View.VISIBLE);
                                } else {
                                    findViewById(R.id.img_ic_logo).setVisibility(View.GONE);

                                    Glide.with(MainActivity.this)
                                            .load(image)
                                            .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                            .into(imageModel);
                                }
                            } else {
                                cardView.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }


        text_box = findViewById(R.id.text_box);
        TextView contadorDeCaracteres = findViewById(R.id.contadorDeCaracteres);

        text_box.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Nada a ser feito antes da mudança de texto
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Atualiza o TextView com o número de caracteres
                int charCount = s.length();
                contadorDeCaracteres.setText(charCount + " / 300");
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Nada a ser feito após a mudança de texto
            }
        });

        text_box = findViewById(R.id.text_box);
        gerarAudio_button = findViewById(R.id.button_gerarAudio);
        queueAux = findViewById(R.id.queue);

        CardView card_mediaPlayer = (CardView) findViewById(R.id.cardView_MP);

        //------ Implementação do botão de Gerar àudio ------//
        //------ Esse botão é responsável por acionar a Api de Gerar a voz, ele usa o método performApiRequest para fazer a requisição ------//
        //------ Esse botão também é responsável por mostrar alguns Alertas para o usuário ------//
        gerarAudio_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("Selecione a categoria...".equals(selectedCategory)) {
                    mostrarAlertaCategoria();
                } else if ("Selecione a voz...".equals(selectedName)) {
                    mostrarAlertaVoz();
                } else if (text_box.getText().toString().length() < 5) {
                    mostrarAlertaTexto();
                } else if (!testInternet()) {
                    mostrarAlertaInternet();
                } else {
                    gerarAudio_button.setVisibility(View.GONE);
                    loadingProgressBar.setVisibility(View.VISIBLE);
                    card_mediaPlayer.setVisibility(View.GONE);

                    if (contadorDeAudios == 5) {
                        avaliaçãoDoApp();
                    } else if (contadorDeAudios == 34) {
                        avaliaçãoDoApp();
                        contadorDeAudios = 6;
                    }
                    contadorDeAudios++;

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ApiRequest apiPostTask = new ApiRequest(MainActivity.this, "7cd85a78a9b1d0355f65005e03dbde36");

                            String responseString;
                            try {
                                responseString = apiPostTask.execute("https://falatron.com/api/app", convertJsonString(selectedName, text_box.getText().toString())).get();
                            } catch (ExecutionException e) {
                                throw new RuntimeException(e);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            startPeriodicUpdate(getTaskId(responseString));
                        }
                    }).start();
                }
            }
        });

        playPause_button = findViewById(R.id.play_button);
        seekBar = findViewById(R.id.seekBar);

        //------ Implementação do botão de play/pause que toque áudio criado ------//
        playPause_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isPlaying) {
                    // Pausar a reprodução
                    mediaPlayer.pause();
                    playPause_button.setBackgroundResource(R.drawable.bg_play_button);
                    if (progressAnimator != null && progressAnimator.isRunning()) {
                        progressAnimator.cancel();
                    }
                } else {
                    // Iniciar a reprodução
                    mediaPlayer.start();
                    playPause_button.setBackgroundResource(R.drawable.bg_pause_button);
                    seekBar.setMax(mediaPlayer.getDuration());

                    updateSeekBar();
                }
                // Faz o botão de play/pause e a Seek Bar reiniciar após o término do áudio
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        playPause_button.setBackgroundResource(R.drawable.bg_play_button);
                        seekBar.setProgress(0);
                        currentProgress = 0;
                        if (progressAnimator != null && progressAnimator.isRunning()) {
                            progressAnimator.cancel();
                        }
                        isPlaying = false;

                        // Reinicie a reprodução do áudio
                        mediaPlayer.seekTo(0);
                    }
                });

                isPlaying = !isPlaying;
            }
        });

        // Atualiza a barra de progressão do áudio (seekBar)
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Se o usuário moveu a SeekBar, atualize a posição do áudio
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Callback quando o usuário toca na SeekBar
                mediaPlayer.pause();
                progressAnimator.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Callback quando o usuário solta a SeekBar
                mediaPlayer.start();
                updateSeekBar();
                playPause_button.setBackgroundResource(R.drawable.bg_pause_button);
            }
        });


        muteButton = findViewById(R.id.volume_on);
        muteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    if (isMuted) {
                        mediaPlayer.setVolume(1.0f, 1.0f);
                        isMuted = false;
                        muteButton.setBackgroundResource(R.drawable.volume_on);
                    } else {
                        mediaPlayer.setVolume(0.0f, 0.0f);
                        isMuted = true;
                        muteButton.setBackgroundResource(R.drawable.volume_off);
                    }
                }
            }
        });

        //------ Implementação do botão de Downloado do áudio, chama o método saveAudioFromBase64, responsável por baixar o áudio no dispositivo ------//
        findViewById(R.id.download_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                } else {
                    saveAudioFromBase64();
                }
            }
        });


        //------ Implementação do botão de Compartilhar áudio, chama o método shareAudio, responsável por compartilhar o áudio em outros Aplicativos ------//
        findViewById(R.id.share_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                } else {
                    shareAudio(base64Audio);
                }
            }
        });

        permissionText = findViewById(R.id.permission_text);
        buttonOk = findViewById(R.id.ok);
        CardView cardAuth = (CardView) findViewById(R.id.card_auth);

        //------ Implementação dos Termos de uso ------//
        cardAuth.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Consumir o evento para evitar cliques fora do CardView
                return true;
            }
        });

        //------ O aplicativo faz uso do sharedPreferences, para guardar as preferências do usuário, quando ele clica em "OK" nos Temos de Uso ------//
        sharedPreferences = getSharedPreferences("Minhas preferências", Context.MODE_PRIVATE);
        boolean userAlreadyPressedOk = sharedPreferences.getBoolean("userPressedOk", false);

        if (userAlreadyPressedOk) {
            // Ocultar o card ou tomar outra ação
            cardAuth.setVisibility(View.GONE);
        } else {
            buttonOk.setEnabled(false);
            CountDownTimer timer = new CountDownTimer(contador * 1000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int secondsLeft = (int) (millisUntilFinished / 1000);
                    String message = "Aguarde " + secondsLeft + " segundos e pressione OK para utilizar o site.";
                    permissionText.setText(message);
                }

                @Override
                public void onFinish() {
                    String message = "\n" + "Pronto, pressione OK.";
                    permissionText.setText(message);
                    buttonOk.setEnabled(true);
                }
            };
            timer.start();
        }

        //------ Implementação do Botão "OK" dos Temos de Uso ------//
        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("userPressedOk", true);
                editor.apply();

                cardAuth.setVisibility(View.GONE);
            }
        });

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        LinearLayout menuDrawer = findViewById(R.id.menuDrawer);

        //------ Implementação do Botão que abre o Menu Lateral ------//
        findViewById(R.id.openMenu_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(menuDrawer);
            }
        });

        //------ Implementação dos Botões que redirecionam os usuário para nossas Redes Sociais ------//

        TextView patreonText = findViewById(R.id.patreon_button);
        final int corOriginalPatreon = patreonText.getCurrentTextColor();

        findViewById(R.id.patreon_button).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Botão pressionado
                    int newColor = getResources().getColor(R.color.blue); // Nova cor ao pressionar o botão
                    patreonText.setTextColor(newColor);
                    openLinkInBrowser("https://www.patreon.com/falatron");
                } else if (event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_CANCEL) {
                    // Botão solto ou ação cancelada
                    patreonText.setTextColor(corOriginalPatreon); // Restaura a cor original do TextView
                }
                return false;
            }
        });

        findViewById(R.id.discord_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLinkInBrowser("https://discord.com/invite/4npUee2XMk");
            }
        });

        findViewById(R.id.twitter_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLinkInBrowser("https://twitter.com/falatronoficial");
            }
        });

        findViewById(R.id.youtube_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLinkInBrowser("https://www.youtube.com/channel/UCfbOooC1RDGxY1s4hTx0geg");
            }
        });

        findViewById(R.id.tiktok_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLinkInBrowser("https://www.tiktok.com/@falatronoficial");
            }
        });

        findViewById(R.id.reddit_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLinkInBrowser("https://www.reddit.com/r/FALATRON/?rdt=59768");
            }
        });

        findViewById(R.id.falatron_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLinkInBrowser("https://falatron.com/");
            }
        });

        findViewById(R.id.compartilhe_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                compartilharApp();
            }
        });

        findViewById(R.id.avalie_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Redirecione o usuário para a página de avaliação na Play Store
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
                } catch (ActivityNotFoundException e) {
                    // Caso a Play Store não esteja instalada, redirecione para a página da Play Store no navegador
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                }
            }
        });
    }

    //------ Metódo que Converte JSON em uma Stringm usado no metodo performApiRequest como parâmetro------//
    public String convertJsonString(String chave1, String chave2) {
        try {
            // Criar um objeto JSON com as chaves e valores desejados
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("voz", chave1);
            jsonObject.put("texto", chave2);

            // Obter a representação do objeto JSON como uma string

            String jsonString = jsonObject.toString();

            // Aqui você pode usar o jsonString conforme necessário
            Log.d("JSON Criado", jsonString);

            return jsonString;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String downloadJson(String url) {
        try {
            // Execute a tarefa para baixar o JSON
            return new ModelsJsonDownloader().execute(url).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getTaskId(String responseString) {
        try {
            JSONObject jsonResponse = new JSONObject(responseString);

            String queue = jsonResponse.getString("queue");
            String task_id = jsonResponse.getString("task_id");

            return task_id;
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao fazer parsing do JSON ou executar a segunda solicitação: " + e.getMessage());
        }
        return "Erro";
    }

    private void getQueue(final String task_id) throws ExecutionException, InterruptedException {
        String voiceValue = "";
        String queueGet = "0";

        final String apiUrl = "https://falatron.com/api/app/" + task_id;

        ApiRequestTask apiRequest = new ApiRequestTask("7cd85a78a9b1d0355f65005e03dbde36");

        try {
            JSONObject jsonResponse = apiRequest.execute(apiUrl).get();
            // Trate a resposta String aqui

            if(jsonResponse.has("voice")){
                voiceValue = jsonResponse.getString("voice");
            } else {
                queueGet = jsonResponse.getString("queue");
            }
            Log.d(TAG, "Resposta da API: " + jsonResponse);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao obter resposta da API: " + e.getMessage());
        }

        String finalQueueGet = queueGet;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView queue = findViewById(R.id.queue);
                queue.setText("Seu lugar na fila é: " + finalQueueGet);

                queue.setVisibility(View.VISIBLE);
            }
        });

        if (!voiceValue.isEmpty()) {
            apiRequest.cancelRequest();
            makeAudio(voiceValue);
        }
    }

    private void startPeriodicUpdate(String task_id) {
        // Executa a primeira solicitação imediatamente
        try {
            getQueue(task_id);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Agenda a execução periódica a cada 10 segundos
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    getQueue(task_id);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // Repete a atualização a cada 10 segundos
                handler.postDelayed(this, 10000);
            }
        }, 10000);
    }

    //------ Método que usa a classe ApiRequest para fazer a requisição na api do Falatron, retorna somente o JSON referente a voz (N/A Emote) ------//
    private void makeAudio(String voiceValue) {

        base64Audio = voiceValue;
        CardView card_mediaPlayer = (CardView) findViewById(R.id.cardView_MP);
        Animation animationCard = AnimationUtils.loadAnimation(this, R.anim.animation_card);
        ScrollView scrollView = findViewById(R.id.scrollView);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final byte[] audioBytes = Base64.decode(voiceValue, Base64.DEFAULT);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        card_mediaPlayer.startAnimation(animationCard);
                        card_mediaPlayer.setVisibility(View.VISIBLE);

                        try {
                            FileOutputStream fos = openFileOutput("temp_audio.mp3", MODE_PRIVATE);
                            fos.write(audioBytes);
                            fos.close();

                            mediaPlayer = new MediaPlayer();
                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mediaPlayer.setDataSource(getFilesDir() + "/temp_audio.mp3");

                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    loadingProgressBar.setVisibility(View.GONE);
                                    queueAux.setVisibility(View.GONE);
                                    gerarAudio_button.startAnimation(animationCard);
                                    gerarAudio_button.setVisibility(View.VISIBLE);
                                    seekBar.setMax(mediaPlayer.getDuration());

                                    AudioNotification.showAudioNotification(MainActivity.this);
                                }
                            });

                            scrollView.post(new Runnable() {
                                @Override
                                public void run() {
                                    scrollView.fullScroll(View.FOCUS_DOWN);
                                }
                            });

                            mediaPlayer.prepareAsync();
                            handler.removeCallbacksAndMessages(null);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }).start();
    }

    private JSONObject updateJsonInAssets(Context context, String assetFileName, JSONObject updatedJsonObject) {
        try {
            // Abra um InputStream para o arquivo JSON original no diretório "assets"
            InputStream inputStream = context.getAssets().open(assetFileName);

            // Crie um OutputStream para a nova cópia do arquivo JSON
            OutputStream outputStream = context.openFileOutput("temp.json", Context.MODE_PRIVATE);

            // Faça a cópia do arquivo original para o novo arquivo
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            // Feche os streams
            inputStream.close();
            outputStream.close();

            // Leia o JSON do arquivo recém-criado
            InputStream updatedInputStream = context.openFileInput("temp.json");
            InputStreamReader inputStreamReader = new InputStreamReader(updatedInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            String originalJson = stringBuilder.toString();

            // Atualize o JSON conforme necessário (por exemplo, usando bibliotecas JSON)
            JSONObject originalJsonObject = new JSONObject(originalJson);
            // Atualize o originalJsonObject com os dados de updatedJsonObject

            // Salve o JSON atualizado no arquivo original na pasta "assets"
            OutputStream assetOutputStream = context.getAssets().openFd(assetFileName).createOutputStream();
            assetOutputStream.write(originalJsonObject.toString().getBytes());
            assetOutputStream.close();

            // Exclua o arquivo temporário
            context.deleteFile("temp.json");

            return originalJsonObject;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    //------ Método para sincronizar o áudio com a Seek Bar(Barra de progressão do áudio) ------//
    private void updateSeekBar() {
        if (mediaPlayer != null) {
            final int maxProgress = mediaPlayer.getDuration();

            if (mediaPlayer.isPlaying()) {
                final int currentProgress = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currentProgress);

                if (progressAnimator != null && progressAnimator.isRunning()) {
                    progressAnimator.cancel();
                }

                progressAnimator = ValueAnimator.ofInt(currentProgress, maxProgress);
                progressAnimator.setDuration(maxProgress - currentProgress);
                progressAnimator.setInterpolator(new LinearInterpolator());

                progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int progress = (int) animation.getAnimatedValue();
                        seekBar.setProgress(progress);
                    }
                });

                progressAnimator.start();

                seekBar.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        seekBar.setProgress(currentProgress);
                    }
                }, 500);
            } else {
                if (progressAnimator != null && progressAnimator.isRunning()) {
                    progressAnimator.cancel();
                }
                // Armazene a posição atual do áudio quando for pausado
                currentProgress = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currentProgress);

            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private String loadJSONFromAsset() {

        String json;

        try {
            InputStream inputStream = getAssets().open("models.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return json;
    }

    //------ Método para baixar o áudio no Dispositivo do Usuário ------//
    private void saveAudioFromBase64() {

        try {
            byte[] audioData = Base64.decode(base64Audio, Base64.DEFAULT);

            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String fileName = text_box.getText().toString().toLowerCase().replace(" ", "_") + ".mp3";
            File file = new File(path, fileName);

            final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


            // Verificar se um  arquivo com mesmo nome já existe na pasta de Downloads
            if (file.exists()) {

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
                dialogBuilder.setTitle("Arquivo já existe");
                dialogBuilder.setMessage("Deseja substituir o arquivo existente?");
                dialogBuilder.setPositiveButton("Substituir", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Substituir o arquivo existente pelo novo
                        try {
                            FileOutputStream outputStream = new FileOutputStream(file);
                            outputStream.write(audioData);
                            outputStream.close();
                            DownloadNotification.showDownloadNotification(MainActivity.this, file.getAbsolutePath());
                            Toast.makeText(getApplicationContext(), "Áudio baixado com sucesso!", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            // Error saving audio file
                        }
                    }
                });
                dialogBuilder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Cancela o download
                        notificationManager.cancel(NOTIFICATION_ID);
                    }
                });

                AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.show();

                // O arquivo não existe, você pode prosseguir com o processo de download e notificação
            } else {

                FileOutputStream outputStream = new FileOutputStream(file);
                outputStream.write(audioData);
                outputStream.close();
                DownloadNotification.showDownloadNotification(MainActivity.this, file.getAbsolutePath());
                Toast.makeText(getApplicationContext(), "Áudio baixado com sucesso!", Toast.LENGTH_SHORT).show();

            }
        } catch (IOException e) {
            e.printStackTrace();
            // Error saving audio file
        }
    }

    //------ Método para compartilhar o áudio com outros Aplicativos ------//
    public void shareAudio(String base64Audio) {

        File tempAudioFile = new File(getFilesDir(), "temp_audio.mp3");
        Uri audioUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tempAudioFile);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("audio/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, audioUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Compartilhar via"));

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                // Apresenta uma mensagem quando a permissão é negada
                showPermissionDeniedMessage();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    //------ Caixa de Dialogo caso o usuário não dê alguma permissão para o Aplicatico ------//
    private void showPermissionDeniedMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissão Negada");
        builder.setMessage("A permissão é necessária para realizar esta ação. Por favor, conceda a permissão nas configurações do aplicativo.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    //------ Caixa de Dialogo caso o usuário não escolher uma Categoria ------//
    private void mostrarAlertaCategoria() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ops!");
        builder.setMessage("Por favor selecione uma categoria.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    //------ Caixa de Dialogo caso o usuário não escolher uma Voz ------//
    private void mostrarAlertaVoz() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ops!");
        builder.setMessage("Por favor selecione uma voz.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    //------ Caixa de Dialogo caso o usuário não digitar uma Texto ------//
    private void mostrarAlertaTexto() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ops!");
        builder.setMessage("Por favor digite um texto entre 5 e 300 caracteres.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    //------ Método para verificar a conectividade de internet ------//
    private boolean testInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    //------ Caixa de Dialogo caso o usuário não esteja conectado à internet ------//
    private void mostrarAlertaInternet() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sem conexão com a Internet");
        builder.setMessage("Ative a conexão de internet para continuar.");

        builder.setPositiveButton("Ativar Internet", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Abra as configurações de rede para que o usuário possa ativar a internet
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            }
        });

        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    //------ Método para abrir um Link Externo ------//
    private void openLinkInBrowser(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void avaliaçãoDoApp() {
        // Verifique se o usuário já selecionou "Nunca pedir novamente" em SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AppRating", MODE_PRIVATE);
        boolean neverAskAgain = prefs.getBoolean("neverAskAgain", false);

        if (!neverAskAgain) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Avalie nosso aplicativo");
            builder.setMessage("Se você gostou deste aplicativo, por gentileza, tire um momento para avaliá-lo. Isso nos ajuda a melhorar continuamente.");
            builder.setPositiveButton("Avaliar Agora", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Redirecione o usuário para a página de avaliação na Play Store
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
                    } catch (ActivityNotFoundException e) {
                        // Caso a Play Store não esteja instalada, redirecione para a página da Play Store no navegador
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                    }
                }
            });
            builder.setNegativeButton("Mais Tarde", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Feche o diálogo e permita que o usuário avalie mais tarde
                    dialog.dismiss();
                }
            });
            builder.setNeutralButton("Nunca", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Marque a opção "Nunca pedir novamente" em SharedPreferences e feche o diálogo
                    SharedPreferences.Editor editor = getSharedPreferences("AppRating", MODE_PRIVATE).edit();
                    editor.putBoolean("neverAskAgain", true);
                    editor.apply();
                    dialog.dismiss();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void compartilharApp() {
        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "\"Envie áudios personalizados com vozes de personagens para seus amigos!\n\n" +
                    "Baixe o Falatron na Play Store: https://play.google.com/store/apps/details?id=" + getPackageName());
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "Compartilhar via"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}