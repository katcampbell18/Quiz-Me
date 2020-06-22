package com.kjc.quizme.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.kjc.quizme.R;
import com.kjc.quizme.database.QuizDbHelper;
import com.kjc.quizme.model.Question;
import com.kjc.quizme.util.Constants;
import com.kjc.quizme.util.Constants.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.kjc.quizme.util.Constants.*;

public class QuizActivity extends AppCompatActivity {

    public static final String EXTRA_SCORE = "extraScore";
    private static final long COUNTDOWN_IN_MILLIS = 30000;

    private TextView questionTextView;
    private TextView scoreTextView;
    private TextView questionCountTextView;
    private TextView categoryTextView;
    private TextView difficultyTextView;
    private TextView counterTextView;
    private RadioGroup rgRadioGroup;
    private RadioButton rb1RadioButton;
    private RadioButton rb2RadioButton;
    private RadioButton rb3RadioButton;
    private Button confirmNextButton;

    private ColorStateList textColorDefaultRb;
    private ColorStateList textColorDefaultCd;

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;

    private ArrayList<Question> questionList;
    private int questionCounter;
    private int questionCountTotal;
    private Question currentQuestion;

    private int score;
    private boolean answered;

    private long backPressedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        questionTextView = findViewById(R.id.question_textView);
        scoreTextView = findViewById(R.id.score_textview);
        questionCountTextView = findViewById(R.id.question_label_textview);
        categoryTextView = findViewById(R.id.category_textview);
        difficultyTextView = findViewById(R.id.difficulty_textview);
        counterTextView = findViewById(R.id.counter_textview);
        rgRadioGroup = findViewById(R.id.radio_group);
        rb1RadioButton = findViewById(R.id.radio_button1);
        rb2RadioButton = findViewById(R.id.radio_button2);
        rb3RadioButton = findViewById(R.id.radio_button3);
        confirmNextButton = findViewById(R.id.confirm_next_button);

        textColorDefaultRb = rb1RadioButton.getTextColors();
        textColorDefaultCd = counterTextView.getTextColors();

        Intent intent = getIntent();
        int categoryID = intent.getIntExtra(StartingScreenActivity.EXTRA_CATEGORY_ID, 0);
        String categoryName = intent.getStringExtra(StartingScreenActivity.EXTRA_CATEGORY_NAME);
        String difficulty = intent.getStringExtra(StartingScreenActivity.EXTRA_DIFFICULTY);

        categoryTextView.setText(getString(R.string.category_label_text) + categoryName);
        difficultyTextView.setText(getString(R.string.difficulty_label_text) + difficulty);

        if (savedInstanceState == null) {
            QuizDbHelper dbHelper = QuizDbHelper.getInstance(this);
            questionList = dbHelper.getQuestions(categoryID, difficulty);
            questionCountTotal = questionList.size();
            Collections.shuffle(questionList);

            showNextQuestion();
        }else {
            questionList = savedInstanceState.getParcelableArrayList(KEY_QUESTION_LIST);
            questionCountTotal = questionList.size();
            questionCounter = savedInstanceState.getInt(KEY_QUESTION_COUNT);
            currentQuestion = questionList.get(questionCounter - 1);
            score = savedInstanceState.getInt(KEY_SCORE);
            timeLeftInMillis = savedInstanceState.getInt(KEY_MILLIS_LEFT);
            answered = savedInstanceState.getBoolean(KEY_ANSWERED);

            if (!answered){
                startCountDown();
            }else {
                updateCountDownText();
                showSolution();
            }
        }

        confirmNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!answered){
                    if (rb1RadioButton.isChecked() || rb2RadioButton.isChecked() || rb3RadioButton.isChecked()){
                        checkAnswer();
                    }else{
                        Toast.makeText(QuizActivity.this, R.string.select_answer_message, Toast.LENGTH_SHORT).show();
                        showNextQuestion();
                    }
                }
            }
        });
    }

    private void showNextQuestion() {
        rb1RadioButton.setTextColor(textColorDefaultRb);
        rb2RadioButton.setTextColor(textColorDefaultRb);
        rb3RadioButton.setTextColor(textColorDefaultRb);
        rgRadioGroup.clearCheck();

        if (questionCounter < questionCountTotal){
            currentQuestion = questionList.get(questionCounter);

            questionCountTextView.setText(currentQuestion.getQuestion());
            rb1RadioButton.setText(currentQuestion.getOption1());
            rb2RadioButton.setText(currentQuestion.getOption2());
            rb3RadioButton.setText(currentQuestion.getOption3());

            questionCounter++;
            questionCountTextView.setText(getString(R.string.question_label_text) + questionCounter + "/" + questionCountTotal);
            answered = false;
            confirmNextButton.setText(R.string.confirm_text_label);

            timeLeftInMillis = COUNTDOWN_IN_MILLIS;
            startCountDown();
        } else{
            finishQuiz();
        }
    }

    private void startCountDown() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                updateCountDownText();
                checkAnswer();
            }
        }.start();
    }

    private void updateCountDownText() {
        int minutes = (int) ((timeLeftInMillis / 1000) / 60);
        int seconds = (int) ((timeLeftInMillis / 1000) % 60);

        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        counterTextView.setText(timeFormatted);

        if (timeLeftInMillis < 10000){
            counterTextView.setTextColor(Color.RED);
        }else{
            counterTextView.setTextColor(textColorDefaultCd);
        }
    }

    private void checkAnswer() {
        answered =true;

        countDownTimer.cancel();

        RadioButton rbSelected = findViewById(rgRadioGroup.getCheckedRadioButtonId());
        int answerNmr = rgRadioGroup.indexOfChild(rbSelected) + 1;

        if (answerNmr == currentQuestion.getAnswerNmr()){
            score++;
            scoreTextView.setText(getString(R.string.score_label_text) + score);
        }
        showSolution();
    }

    private void showSolution() {
        rb1RadioButton.setTextColor(Color.RED);
        rb2RadioButton.setTextColor(Color.RED);
        rb3RadioButton.setTextColor(Color.RED);

        switch (currentQuestion.getAnswerNmr()) {
            case 1:
                rb1RadioButton.setTextColor(Color.GREEN);
                questionTextView.setText(R.string.answer1_correct_text);
                break;
            case 2:
                rb2RadioButton.setTextColor(Color.GREEN);
                questionTextView.setText(R.string.answer2_correct_text);
                break;
            case 3:
                rb3RadioButton.setTextColor(Color.GREEN);
                questionTextView.setText(R.string.answer3_correct_text);
                break;
        }

        if (questionCounter < questionCountTotal){
            confirmNextButton.setText(R.string.next_button_text);
        } else {
            confirmNextButton.setText(R.string.finish_button_text);
        }
    }

    private void finishQuiz() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_SCORE, score);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()){
            finishQuiz();
        }else{
            Toast.makeText(this, R.string.press_back_message, Toast.LENGTH_SHORT).show();
        }
        backPressedTime = System.currentTimeMillis();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null){
            countDownTimer.cancel();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putInt(KEY_SCORE, score);
        outState.putInt(KEY_QUESTION_COUNT, questionCounter);
        outState.putLong(KEY_MILLIS_LEFT, timeLeftInMillis);
        outState.putBoolean(KEY_ANSWERED, answered);
        outState.putParcelableArrayList(KEY_QUESTION_LIST, questionList);
    }
}