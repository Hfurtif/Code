package com.example.engine

data class LevelTestQuestion(
    val id: Int,
    val questionEn: String,
    val questionFr: String,
    val levelTarget: String,
    val sampleGoodAnswers: List<String>
)

data class RoleplayScenario(
    val id: String,
    val title: String,
    val roleCode: String,
    val roleUser: String,
    val context: String,
    val openingLine: String
)

data class PronunciationExercise(
    val word: String,
    val phonetic: String,
    val translation: String,
    val tip: String,
    val category: String = "Mots pièges",
    val syllables: String = ""
) {
    val phoneticIpa: String get() = phonetic
}

object CurriculumRepository {
    val pronunciationBank: List<com.example.engine.PronunciationExercise> get() = pronunciationDrills


    val levelQuestions = listOf(
        LevelTestQuestion(
            id = 1,
            questionEn = "Can you introduce yourself in a few words? What is your name and where are you from?",
            questionFr = "Peux-tu te présenter brièvement ? Ton prénom et d'où tu viens ?",
            levelTarget = "A1",
            sampleGoodAnswers = listOf("My name is", "I come from", "I live in")
        ),
        LevelTestQuestion(
            id = 2,
            questionEn = "What do you usually do in your free time or during weekends?",
            questionFr = "Que fais-tu généralement pendant ton temps libre ou le week-end ?",
            levelTarget = "A2",
            sampleGoodAnswers = listOf("I like to", "I usually read", "I spend time with")
        ),
        LevelTestQuestion(
            id = 3,
            questionEn = "Tell me about a memorable trip you took in the past. What happened?",
            questionFr = "Raconte-moi un voyage mémorable que tu as fait. Qu'est-il arrivé ?",
            levelTarget = "B1",
            sampleGoodAnswers = listOf("I visited", "It was great because", "We had a lot of fun")
        ),
        LevelTestQuestion(
            id = 4,
            questionEn = "If you could change one habit in your daily routine, what would it be and why?",
            questionFr = "Si tu pouvais changer une habitude dans ton quotidien, ce serait quoi et pourquoi ?",
            levelTarget = "B2",
            sampleGoodAnswers = listOf("If I could change", "I would wake up earlier because", "It would help me")
        ),
        LevelTestQuestion(
            id = 5,
            questionEn = "How do you think artificial intelligence and voice assistants will impact education in the next decade?",
            questionFr = "Comment penses-tu que l'intelligence artificielle va impacter l'éducation ces prochaines années ?",
            levelTarget = "C1",
            sampleGoodAnswers = listOf("In my perspective", "It offers personalized learning", "However there are challenges")
        )
    )

    val roleplays = listOf(
        RoleplayScenario(
            id = "london_cafe",
            title = "Commander dans un café à Londres",
            roleCode = "Barista souriant à Camden",
            roleUser = "Client pressé mais poli",
            context = "Tu viens commander un café et un croissant.",
            openingLine = "Hi there! Welcome to The Camden Roast. What can I get started for you today?"
        ),
        RoleplayScenario(
            id = "airport_checkin",
            title = "Enregistrement à l'aéroport JFK",
            roleCode = "Agent d'embarquement",
            roleUser = "Passager pour Paris",
            context = "Tu enregistres tes bagages et demandes une place côté fenêtre.",
            openingLine = "Good afternoon! Can I see your passport and booking reference, please?"
        ),
        RoleplayScenario(
            id = "job_interview",
            title = "Entretien d'embauche en anglais",
            roleCode = "Manager RH bienveillant",
            roleUser = "Candidat motivé",
            context = "Tu expliques tes compétences et ta façon de résoudre les imprévus.",
            openingLine = "Thank you for coming in today. Could you walk me through your background and what motivates you?"
        ),
        RoleplayScenario(
            id = "doctor_visit",
            title = "Consultation médicale",
            roleCode = "Médecin généraliste",
            roleUser = "Patient avec un mal de tête persistant",
            context = "Tu décris tes symptômes et depuis combien de temps tu as mal.",
            openingLine = "Hello, take a seat. How have you been feeling lately, and what brings you here?"
        )
    )

    val pronunciationDrills = listOf(
        PronunciationExercise("Comfortable", "/ˈkʌm.fə.tə.bəl/", "Confortable", "Prononce 'comf-tuh-bul' en 3 syllabes, pas 4.", "Mots pièges", "comf-ter-buhl"),
        PronunciationExercise("Thoroughly", "/ˈθʌr.ə.li/", "Minutieusement", "La langue touche les dents du haut pour le son 'TH' doux.", "Sons TH & R", "thur-uh-lee"),
        PronunciationExercise("Schedule", "/ˈskedʒ.uːl/", "Emploi du temps", "En américain commence par 'SK', en britannique souvent par 'SH'.", "Mots pièges", "sked-jool"),
        PronunciationExercise("Entrepreneur", "/ˌɒn.trə.prəˈnɜːr/", "Entrepreneur", "Accent tonique sur la dernière syllabe.", "Mots pièges", "on-truh-pruh-nur"),
        PronunciationExercise("Vulnerable", "/ˈvʌl.nər.ə.bəl/", "Vulnérable", "Relâche le son 'ner' au milieu.", "Mots pièges", "vuhl-ner-uh-buhl"),
        PronunciationExercise("Wednesday", "/ˈwenz.deɪ/", "Mercredi", "Le 'd' et le premier 'e' sont complètement muets.", "Lettres muettes", "wenz-day"),
        PronunciationExercise("Doubt", "/daʊt/", "Doute", "Le 'b' est silencieux, le mot rime exactement avec 'out'.", "Lettres muettes", "dowt"),
        PronunciationExercise("Island", "/ˈaɪ.lənd/", "Île", "Le 's' ne se prononce absolument jamais.", "Lettres muettes", "eye-lund"),
        PronunciationExercise("Muscle", "/ˈmʌs.əl/", "Muscle", "Le 'c' est complètement muet, prononce 'muss-el'.", "Lettres muettes", "muh-sul"),
        PronunciationExercise("Subtle", "/ˈsʌt.əl/", "Subtil", "Le 'b' est muet, prononce 'sut-tuhl'.", "Lettres muettes", "suh-tuhl"),
        PronunciationExercise("Squirrel", "/ˈskwɜːr.əl/", "Écureuil", "Enchaîne le 'skw' et le 'r' sans intercaler de son 'i'.", "Sons TH & R", "skwur-uhl"),
        PronunciationExercise("Breathe", "/briːð/", "Respirer", "Son 'TH' voisé qui vibre contre les cordes vocales.", "Sons TH & R", "breedh"),
        PronunciationExercise("Specifically", "/spəˈsɪf.ɪ.kli/", "Spécifiquement", "L'accent tonique principal frappe la syllabe 'SIF'.", "Rythme & Accent", "spuh-sif-ik-lee"),
        PronunciationExercise("Architecture", "/ˈɑːr.kɪ.tek.tʃər/", "Architecture", "Le groupe 'ch' se prononce comme un 'K' dur.", "Rythme & Accent", "ahr-ki-tek-cher"),
        PronunciationExercise("Recipe", "/ˈres.ə.pi/", "Recette de cuisine", "Le 'e' final se prononce clairement 'i' (trois syllabes).", "Mots pièges", "res-i-pee")
    )


    val personalGrowthPrompts = listOf(
        "Aujourd'hui, concentrons-nous sur une petite victoire. Quelle est une tâche importante que tu aimerais accomplir aujourd'hui ?",
        "Parlons de gestion de l'énergie. As-tu pris 5 minutes pour respirer calmement et faire une vraie pause ce matin ?",
        "La confiance se construit par la répétition. Quel défi récent as-tu relevé même si c'était inconfortable ?"
    )

    val psychologyPrompts = listOf(
        "L'écoute active est la clé des relations sereines. Quand quelqu'un te parle, essaie de reformuler ce qu'il ressent avant de donner ton avis.",
        "Exercice de communication : au lieu de dire 'Tu ne m'écoutes jamais', essaie 'Je me sens un peu frustré quand je ne finis pas ma phrase'. Qu'en penses-tu ?",
        "Rappelle-toi : tes émotions sont des signaux précieux, pas des ennemis. Accueille-les sans jugement."
    )
}
