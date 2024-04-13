package com.example.notKahoot.ui.createGame.playGame

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.example.notKahoot.MainActivity
import com.example.notKahoot.R
import com.example.notKahoot.data.QuizDatabase
import com.example.notKahoot.databinding.FragmentGameplayQuizBinding
import com.example.notKahoot.model.QuestionModel
import com.example.notKahoot.ui.viewModel.NearbyConnectionModel
import com.example.notKahoot.ui.viewModel.NearbyConnectionWrapper
import com.google.android.gms.nearby.connection.Payload
import com.google.gson.Gson
import com.google.gson.JsonParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

var score = 0

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class GameplayQuizFragment : Fragment() {
    private lateinit var appDb: QuizDatabase

    /**
     *
     */
    class QuestionResponses(val questionId: Int,
                            // Client UUID: answer index
                            var responses: MutableMap<UUID, String>
    )

    /**
     *
     */
    class VM: ViewModel() {
        /// Not sure if this is the right way of doing things, but onCreateView should only
        /// populate the view model if this is false
        var isInitialized: Boolean = false

        var selectedOptionString: String? = null
        var correctOptionString: String? = null
        /// MARK - server
        var questions: List<QuestionModel> = emptyList()
        var questionIndex: Int = 0
        var responses: MutableList<QuestionResponses> = mutableListOf()
        // Nearby connection ID: (UUID, nearby connection name)
        var idMap: MutableMap<String, Pair<UUID, String>> = mutableMapOf()
        /// On the screen where the correct answer has been revealed
        var showCorrectAnswer = false

        // MARK - client and server
        var questionId: Int? = null
        var currentQuestion: QuestionModel? = null
        var answerConfirmed: Boolean = false
    }

    private val originalOptionTextColor = R.color.originalOption
    private val selectedOptionTextColor = R.color.selectedOption
    private val resultsOptionTextColor = R.color.white
//    private val correctOptionTextColor = Color.parseColor("#82FF7E")
//    private val incorrectOptionTextColor = Color.parseColor("#FF8888")

    private var _binding: FragmentGameplayQuizBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    /// Need to be identify yourself when you get a broadcast message, but there is no way to
    /// get your own nearby connection ID and getting the server to send it to you sounds like a pain
    private val id: UUID get() = activityVm.uuid

    private val activityVm: ActivityGameplayViewModel by activityViewModels()
    private val vm: VM by viewModels()

    /**
     *
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameplayQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     *
     */
    private fun populateOptions(question: QuestionModel) {
        binding.currentQuestion.text = question.question

        val choices = arrayOf(question.option_one, question.option_two, question.option_three, question.option_four)
        questionWidgets.forEachIndexed { i, option ->
            // Ensures any previous made invisible widgets are visible
            // Also added the option to gray out empty buttons (alpha) but these are still selectable so ignore it
//            option.visibility = View.VISIBLE
//            if (choices[i].isEmpty()) {
//                option.alpha = 0.25F
//            } else {
//                option.alpha = 1F
//            }
            option.text = choices[i]
        }
    }

    /**
     * Updates the question or if there are no questions left moves the player onto the results fragment
     */
    private fun serverChangeQuestion(increment: Boolean) {
        // Go to results screen if it's the end of questions Array
        if (increment) {
            vm.questionIndex += 1
            vm.selectedOptionString = null
            vm.correctOptionString = null
        }

        if (vm.questionIndex >= vm.questions.size) {
            lifecycleScope.launch {
                val results = makeResultsPayload()
                activityVm.quizResults = results
                sendQuizResults(results)
                delay(500)

                findNavController().navigate(R.id.action_FragmentGameplayQuiz_to_FragmentGameplayResults)
            }
            return
        }

        val question = rearrangeQuestionOptions(vm.questions[vm.questionIndex])
        populateOptions(question)

        setOptionsColor()
        updateNumAnsweredText()
    }

    /**
     *
     */
    private val questionWidgets by lazy {
        arrayListOf(binding.questionOption1, binding.questionOption2,
            binding.questionOption3, binding.questionOption4)
    }

    private val questionWidgetTextPair: List<Pair<TextView, String>>
        get() = (currentQuestion?.allOptions ?: listOf("", "", "", "")).mapIndexed { i, text ->
            Pair(questionWidgets[i], text)
        }

    /**
     * Logic to rearrange questions
     * Assumes that only option two or three will be blank
     */
    private fun rearrangeQuestionOptions(question: QuestionModel): QuestionModel {
        var rearrangedQuestion = question

        for (questionWidget in questionWidgets) {
            questionWidget.visibility = View.VISIBLE
        }

        if (question.option_two.isEmpty() && question.option_three.isEmpty()) {
            binding.questionOption3.visibility = View.INVISIBLE
            binding.questionOption4.visibility = View.INVISIBLE
            rearrangedQuestion = QuestionModel(
                question.id,
                question.quiz_id,
                question.question,
                question.option_one,
                question.option_four,
                question.option_two,
                question.option_three,
                question.correct_option
            )
        } else if (question.option_three.isEmpty() && question.option_four.isEmpty()) {
            // Client player only
            binding.questionOption3.visibility = View.INVISIBLE
            binding.questionOption4.visibility = View.INVISIBLE
        } else if (question.option_two.isEmpty()) {
            binding.questionOption4.visibility = View.INVISIBLE
            rearrangedQuestion = QuestionModel(
                question.id,
                question.quiz_id,
                question.question,
                question.option_one,
                question.option_three,
                question.option_four,
                question.option_two,
                question.correct_option
            )
        } else if (question.option_three.isEmpty()) {
            binding.questionOption4.visibility = View.INVISIBLE
            rearrangedQuestion = QuestionModel(
                question.id,
                question.quiz_id,
                question.question,
                question.option_one,
                question.option_two,
                question.option_four,
                question.option_three,
                question.correct_option
            )
        } else if (question.option_four.isEmpty()) {
            // Client player only
            binding.questionOption4.visibility = View.INVISIBLE
        }

        return rearrangedQuestion
    }

    /**
     *
     */
    private val currentQuestion: QuestionModel? get() {
        if (activityVm.isServer) {
            if (vm.questionIndex < vm.questions.size) {
                // Logic to rearrange questions
                return rearrangeQuestionOptions(vm.questions[vm.questionIndex])
            }
        } else {
            return vm.currentQuestion?.let { rearrangeQuestionOptions(it) }
        }
        return null
    }

    /**
     *
     */
    private fun setOptionsColor() {
        val view = view ?: return
        questionWidgetTextPair.forEach { option ->
            val optionWidget = option.first
            val optionText = option.second
            val selected = optionText == vm.selectedOptionString
            val correct = optionText == vm.correctOptionString
            val incorrect = selected && (vm.correctOptionString != null && optionText != vm.correctOptionString)

            val color = if (correct || incorrect) resultsOptionTextColor
                else if (selected) selectedOptionTextColor
                else originalOptionTextColor

            optionWidget.setTextColor(resources.getColor(color))
            optionWidget.background = ContextCompat.getDrawable(
                view.context,
                if (correct) R.drawable.correct_option_border
                else if (incorrect) R.drawable.wrong_option_border
                else if (selected) R.drawable.selected_option_border
                else R.drawable.default_option_border,
            )

            // If answer has been sent, can no longer modify
            optionWidget.isEnabled = !vm.answerConfirmed
        }

        binding.btnAnswerConfirm.isEnabled = vm.selectedOptionString != null && vm.answerConfirmed == false && vm.showCorrectAnswer == false
        binding.btnEndRound.text = if (vm.showCorrectAnswer) "Next round" else "End round"

        binding.progressBar.progress = vm.questionIndex
    }

    /**
     *
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val questionsRandomised = prefs.getBoolean("question_order", false)

        if (!vm.isInitialized) {
            vm.isInitialized = true

            if (activityVm.isServer) {
                appDb = QuizDatabase.getDatabase(requireContext())
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val questions = appDb.questionDao().filterByQuiz(activityVm.quizId)
                        withContext(Dispatchers.Main) {
                            vm.questions = questions
                            if (questionsRandomised) {
                                vm.questions = vm.questions.shuffled()
                            }
                            if (vm.questions.isEmpty()) {
                                // TODO: show a warning or something
                                Log.d(TAG, "Got empty question array for quiz(id = ${activityVm.quizId})")

                                vm.questions = arrayListOf(
                                    QuestionModel(1, 1, "Empty Quiz, should you:", "Leave empty", "", "", "Add questions", "Add questions")
                                )
                            }
                            currentQuestion?.let { sendQuestion(QuestionPayload(it, vm.questionIndex, vm.questions.size, id)) }

                            // Remove the loadingBar for the host by default
                            binding.loadingBar.visibility = View.GONE

                            // Initial question when host user presses "Start quiz"
                            serverChangeQuestion(false)
                        }
                    }

                    binding.progressBar.max = vm.questions.size
               }
            }
        } else {
            if (activityVm.isServer) {
                serverChangeQuestion(false)
                binding.progressBar.max = vm.questions.size
                binding.loadingBar.visibility = View.GONE
            } else {
                // Hides the questions and confirm button for the non-host players
                currentQuestion?.let { populateOptions(it) }
                binding.areaToHide.visibility = View.INVISIBLE
                binding.textNumConfirmedAnswers.isGone = true
                setOptionsColor()
            }
        }

        if (!activityVm.isServer) {
            binding.loadingBar.visibility = if (currentQuestion != null) View.GONE else View.VISIBLE
            binding.areaToHide.visibility = if (currentQuestion != null) View.VISIBLE else View.GONE
            binding.progressBar.visibility = if (currentQuestion != null) View.VISIBLE else View.GONE
        }

        activityVm.connection.delegate = delegate

        // Add color changing listener in all options
        questionWidgets.forEachIndexed { index, widget ->
            widget.setOnClickListener {
                vm.selectedOptionString = currentQuestion?.allOptions?.get(index)
                setOptionsColor() // To prevent multi-selection
            }
        }

//        questionWidgetTextPair.forEach {
//        }

        binding.btnAnswerConfirm.setOnClickListener {
            // TODO show loading overlay while waiting for answer
            vm.selectedOptionString?.let { it ->
                if (activityVm.isServer) {
                    vm.answerConfirmed = true
                    currentQuestion?.id?.let { questionId ->
                        var index =
                            vm.responses.indexOfFirst { it.questionId == questionId }
                        if (index == -1) {
                            index = vm.responses.size
                            vm.responses.add(QuestionResponses(questionId, mutableMapOf()))
                        }
                        vm.responses[index].responses[id] = it
                    }
                    setOptionsColor()
                    updateNumAnsweredText()
                } else {
                    sendAnswer(it)
                    setOptionsColor()
                }
            }
        }

        // Game host only
        binding.btnEndRound.isVisible = activityVm.isServer
        binding.btnEndRound.setOnClickListener {
            print(vm.showCorrectAnswer)
            if (vm.showCorrectAnswer) {
                // Go to next question
                vm.showCorrectAnswer = !vm.showCorrectAnswer
                serverChangeQuestion(true)
                currentQuestion?.let {
                    sendQuestion(QuestionPayload(it, vm.questionIndex, vm.questions.size, id))
                    vm.answerConfirmed = false
                    vm.selectedOptionString = null
                    vm.correctOptionString = null
                    populateOptions(it)
                    setOptionsColor()
                }
            } else {
                currentQuestion?.let {
                    vm.showCorrectAnswer = !vm.showCorrectAnswer
                    vm.correctOptionString = it.correct_option
                    // Show the correct answer
                    binding.progressBar.progress = vm.questionIndex + 1
                    sendQuestionFeedback(AnswerFeedbackPayload(it.id, it.correct_option, id))
                    setOptionsColor()
                }
            }
        }
    }

    /**
     *
     */
    open class ToohakPayload(val type: String,
                             /// Can't get your own nearby connection ID, so create a UUID instead
                             val id: UUID,
                             val version: Int = 1) {
        companion object {
            const val QUESTION = "QUESTION"
            // correct answer to the question
            const val ANSWER_FEEDBACK = "ANSWER_FEEDBACK"

            // client response to a question
            const val ANSWER = "ANSWER"

            // Results sent by the server to at the end of the quiz
            const val QUIZ_RESULTS = "QUIZ_RESULTS"
        }

    }

    /**
     *
     */
    class QuestionPayload(
        /// ID of the question
        val questionId: Int,

        // Question description
        val question: String,

        /// Options names
        val choice_1: String,
        val choice_2: String,
        val choice_3: String,
        val choice_4: String,

        /// Time question was started, in milliseconds since 1970/01/01 using the server's clock
        val sentTimestamp: Long = System.currentTimeMillis(),

        /// Max duration for a question
        val duration: Int = 10,

        val progress: Int = 0,
        val maxQuestions: Int = 1,

        id: UUID,
    ): ToohakPayload(type = QUESTION, id = id) {
        constructor(question: QuestionModel, progress: Int, maxQuestions: Int, id: UUID) : this(
            question.id,
            question.question,
            question.option_one,
            question.option_two,
            question.option_three,
            question.option_four,
            progress = progress,
            maxQuestions = maxQuestions,
            id = id
        )
    }

    /**
     *
     */
    class AnswerPayload(
        /// ID of the question
        val questionId: Int,

        /// String value of the answer
        val answer: String,
        id: UUID
    ): ToohakPayload(type = ToohakPayload.ANSWER, id = id)

    /**
     *
     */
    class AnswerFeedbackPayload(
        /// ID of the question
        val questionId: Int,

        /// Index of the option that results in the correct answer
        val correctOptionString: String,
        id: UUID
    ): ToohakPayload(type = ToohakPayload.ANSWER_FEEDBACK, id = id)

    class QuizResultsPayload(
        val results: List<IndividualResults>,
        id: UUID
    ): ToohakPayload(type = ToohakPayload.QUIZ_RESULTS, id) {
        class IndividualResults(
            val id: UUID,
            val name: String,
            val numCorrect: Int,
            val numAnswered: Int
        )
    }

    private fun makeResultsPayload(): QuizResultsPayload {
        // UUID: name
        val reverseMap = vm.idMap.entries.associateBy({ it.value.first }) { it.value.second }
        // Totals for each user (correct/answered)
        val totals = mutableMapOf<UUID, Pair<Int, Int>>()
        vm.responses.forEach { responses ->
            val question = vm.questions.find { it.id == responses.questionId }
            if (question != null) {
                responses.responses.forEach { (uuid, answer) ->
                    val current = totals[uuid] ?: Pair(0, 0)
                    totals[uuid] = Pair(
                        current.first + (if (answer == question.correct_option) 1 else 0),
                        current.second + 1
                    )
                }
            }
        }

        val context = requireContext()
        val sortedTotals = totals.map { (key, total) ->
            var name = reverseMap[key] ?: context.getString(R.string.unknown_name)
            if (key == id) name = activityVm.name ?: context.getString(R.string.unknown_server_name)
            QuizResultsPayload.IndividualResults(key, name, total.first, total.second)
        }.sortedByDescending { it.numCorrect }

        return QuizResultsPayload(sortedTotals, id)
    }

    /**
     *
     */
    private val TAG = "FragmentGameplayQuiz"
    private val gson = Gson()
    private fun sendQuestion(question: QuestionPayload) {
        assert(activityVm.isServer) { "Only valid for server instances" }
        val gson = Gson()
        val json = gson.toJson(question)
        Log.d(TAG, "Send question $json")
        activityVm.connection.send(Payload.fromBytes(json.toByteArray(Charsets.UTF_8)))
    }

    /**
     *
     */
    private fun sendQuestionFeedback(answer: AnswerFeedbackPayload) {
        assert(activityVm.isServer) { "Only valid for server instances" }
        val gson = Gson()
        val json = gson.toJson(answer)
        Log.d(TAG, "Send answer feedback $json")
        activityVm.connection.send(Payload.fromBytes(json.toByteArray(Charsets.UTF_8)))
    }

    /**
     *
     */
    private fun sendAnswer(optionText: String) {
        assert(!activityVm.isServer) { "Only valid for client instances" }
        val questionId = vm.questionId
        if (questionId == null) {
            Log.d(TAG, "Question ID was null when trying to send answer")
            return
        }
        val json = gson.toJson(AnswerPayload(questionId, optionText, id))
        Log.d(TAG, "Send answer $json")
        activityVm.connection.send(Payload.fromBytes(json.toByteArray(Charsets.UTF_8)))
        vm.answerConfirmed = true
        setOptionsColor()
    }

    private fun sendQuizResults(results: QuizResultsPayload) {
        assert(activityVm.isServer) { "Only valid for server instances" }
        val gson = Gson()
        val json = gson.toJson(results)
        Log.d(TAG, "Send results payload $json")
        activityVm.connection.send(Payload.fromBytes(json.toByteArray(Charsets.UTF_8)))
    }

    val model: NearbyConnectionModel get() = activityVm.connection

    /**
     *
     */
    private fun onMainThread(task: () -> Unit) {
        lifecycleScope.launch { withContext(Dispatchers.Main) { task() } }
    }

    private val numResponses: Pair<Int, Int>? get() {
        val question = currentQuestion ?: return null
        // vm.responses also contains the server's answers
        val answered = vm.responses.find { it.questionId == question.id }?.responses?.size ?: 0
        val total = (activityVm.connection.connectedEndpoints.value?.count() ?: 0) + 1
        return Pair(answered, total)
    }

    private fun updateNumAnsweredText() {
        numResponses?.let {
            binding.textNumConfirmedAnswers.text = requireContext().getString(R.string.num_responses, it.first, it.second)
        }
    }


    /**
     * Big method to handle connections
     */
    private val delegate = object : NearbyConnectionModel.NearbyConnectionDelegate() {
        private fun onEndpointDisconnected(debugEndpointDescription: String) {
            val context = context ?: return
            onMainThread {
                if (activityVm.isServer) {
                    Toast.makeText(context, "$debugEndpointDescription was disconnected", Toast.LENGTH_LONG)
                        .show()
                    updateNumAnsweredText()
                } else {
                    Toast.makeText(context, R.string.disconnection, Toast.LENGTH_LONG).show()
                }

                if (!activityVm.isServer || model.connectedEndpoints.value?.isEmpty() == true) {
                    activity?.let {
                        // Return to home: end the quiz
                        activityVm.connection.stopAllEndpoints()
                        val intent = Intent(it, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                    }
                }
            }
        }

        override fun onEndpointDisconnected(endpoint: NearbyConnectionWrapper.Endpoint) {
            onEndpointDisconnected(endpoint.toString())
        }
        override fun onConnectionFailed(endpointId: String, exception: Exception) {
            onEndpointDisconnected("Endpoint(id=$endpointId)")
        }

        override fun onReceive(endpoint: NearbyConnectionWrapper.Endpoint, payload: Payload) {
            // https://developers.google.com/nearby/connections/android/exchange-data
            // Unlike files or streams, byte types are sent in a single packet
            assert(payload.type == Payload.Type.BYTES) { "Only supporting byte payloads at the moment" }

            payload.asBytes()?.let { bytes ->
                val json = String(bytes, Charsets.UTF_8)
                Log.d(TAG, "Received following string from $endpoint: $json")
                if (activityVm.isServer) onReceiveServer(endpoint, json)
                else onReceiveClient(endpoint, json)
            }
        }

        private fun <T> gsonTryParse(json: String, asClass: Class<T>): T? {
            return try {
                gson.fromJson(json, asClass)
            } catch (e: JsonParseException) {
                null
            }
        }

        fun onReceiveServer(endpoint: NearbyConnectionWrapper.Endpoint, json: String) {
            try {
                val type = gson.fromJson(json, ToohakPayload::class.java)?.type ?: return
                when (type) {
                    ToohakPayload.ANSWER -> gson.fromJson(json, AnswerPayload::class.java)?.let {
                        onReceiveAnswer(endpoint, it)
                    }
                }
            } catch(e: JsonParseException) {
                Log.d(TAG, "Failed to parse JSON", e)
            }
        }

        fun onReceiveAnswer(endpoint: NearbyConnectionWrapper.Endpoint, answer: AnswerPayload) {
            val currentQuestionId = currentQuestion?.id
            if (currentQuestionId == null) {
                Log.d(TAG, "Could not determine current question")
                return
            }

            if (answer.questionId != currentQuestionId) {
                Log.d(TAG, "Answer for question ${answer.questionId} received from $endpoint" +
                "when current question is $currentQuestionId")
                return
            }

            onMainThread {
                var index = vm.responses.indexOfFirst { it.questionId == currentQuestionId }
                if (index == -1) {
                    index = vm.responses.size
                    vm.responses.add(QuestionResponses(currentQuestionId, mutableMapOf()))
                }

                if (vm.idMap[endpoint.id] == null) {
                    vm.idMap[endpoint.id] = Pair(answer.id, endpoint.name)
                }

                if (vm.idMap[endpoint.id]?.first == answer.id) {
                    vm.responses[index].responses[answer.id] = answer.answer
                } else {
                    Log.d(TAG, "Client ID for user $endpoint changed from ${vm.idMap[endpoint.id]?.first} to ${answer.id}")
                }



                updateNumAnsweredText()
//                if (vm.responses[index].responses.size == activityVm.connection.connectedEndpoints.value?.size) {
                // AND server has answered as well
//                    // All clients answered
//                }
            }
        }

        fun onReceiveClient(endpoint: NearbyConnectionWrapper.Endpoint, json: String) {
            try {
                val type = gson.fromJson(json, ToohakPayload::class.java)?.type ?: return
                when (type) {
                    ToohakPayload.QUESTION -> gson.fromJson(json, QuestionPayload::class.java)?.let {
                        onReceiveQuestion(endpoint, it)
                    }
                    ToohakPayload.ANSWER_FEEDBACK -> gson.fromJson(json, AnswerFeedbackPayload::class.java)?.let {
                        onReceiveFeedback(endpoint, it)
                    }
                    ToohakPayload.QUIZ_RESULTS -> gson.fromJson(json, QuizResultsPayload::class.java)?.let {
                        onReceiveQuizResults(endpoint, it)
                    }
                }
            } catch(e: JsonParseException) {
                Log.d(TAG, "Failed to parse JSON", e)
            }
        }
        fun onReceiveQuestion(endpoint: NearbyConnectionWrapper.Endpoint, questionPayload: QuestionPayload) {
            // Delta between the clocks of the two phones, plus communication delay
            // Can use to approximate time remaining. Would ideally want to use round trip time but
            // that's too much work
            val clockDelta = System.currentTimeMillis() - questionPayload.sentTimestamp
            val options = arrayOf("", "", "", "")
            val choices = arrayOf(questionPayload.choice_1, questionPayload.choice_2, questionPayload.choice_3, questionPayload.choice_4)

            // Used for the client player to update their progress bar
            binding.progressBar.max = questionPayload.maxQuestions
            // TODO confirm this doesn't mess up anything
            vm.questionIndex = questionPayload.progress
            binding.progressBar.progress = questionPayload.progress

            choices.forEachIndexed { i, option ->
                options[i] = option
            }
            val questionModel = QuestionModel(questionPayload.questionId,-1, questionPayload.question, options[0], options[1], options[2], options[3], "")
            vm.currentQuestion = questionModel
            vm.questionId = questionModel.id
            vm.answerConfirmed = false
            vm.selectedOptionString = null
            vm.correctOptionString = null

            // Remove loading bar (for client), show questions
            binding.areaToHide.visibility = View.VISIBLE
            binding.progressBar.visibility = View.VISIBLE
            binding.loadingBar.visibility = View.GONE
            populateOptions(questionModel)
            setOptionsColor()
        }

        fun onReceiveFeedback(endpoint: NearbyConnectionWrapper.Endpoint, feedback: AnswerFeedbackPayload) {
            if (feedback.questionId == vm.questionId) {
                Log.d(TAG, "Correct answer to Q${feedback.questionId} was options[index = ${feedback.correctOptionString}]")
                vm.correctOptionString = feedback.correctOptionString
                setOptionsColor()
            } else {
                Log.d(TAG, "Got feedback to question ${feedback.questionId} when on question ${vm.questionId}")
            }
        }

        /**
         *
         */
        fun onReceiveQuizResults(endpoint: NearbyConnectionWrapper.Endpoint, results: QuizResultsPayload) {
            activityVm.quizResults = results
            findNavController().navigate(R.id.action_FragmentGameplayQuiz_to_FragmentGameplayResults)
        }

    }


    /**
     *
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
