package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ConversationMessage
import com.example.data.model.DownloadedDocument
import com.example.data.model.DownloadedKnowledge
import com.example.data.model.LearnedError
import com.example.data.model.Lesson
import com.example.data.model.PendingTask
import com.example.data.model.ResourcePack
import com.example.data.model.UserProfile
import com.example.data.model.Vocabulary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfile::class,
        ConversationMessage::class,
        Lesson::class,
        LearnedError::class,
        Vocabulary::class,
        ResourcePack::class,
        PendingTask::class,
        DownloadedKnowledge::class,
        DownloadedDocument::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun lessonDao(): LessonDao
    abstract fun learnedErrorDao(): LearnedErrorDao
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun resourceDao(): ResourceDao
    abstract fun pendingTaskDao(): PendingTaskDao
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "code_teacher_db"
                ).fallbackToDestructiveMigration(true)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed initial data
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getDatabase(context)
                            seedInitialData(database)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedInitialData(database: AppDatabase) {
            // Seed profile
            database.userDao().insertOrUpdate(
                UserProfile(
                    id = 1,
                    name = "Alex",
                    level = "B1",
                    objective = "Discussion & Savoir",
                    streakDays = 1,
                    xpScore = 150,
                    voiceGender = "female",
                    voiceAccent = "fr-FR",
                    speechRate = 1.0f,
                    speechPitch = 1.0f,
                    continuousMode = true,
                    correctionFrequency = "normal",
                    explanationLang = "fr",
                    hasCompletedOnboarding = true,
                    preferredLanguageCode = "fr-FR",
                    preferredLanguageName = "Français",
                    activeRole = "friend",
                    hasSelectedInitialLanguage = false,
                    maxStorageLimitGb = 20.0,
                    companionName = "Code",
                    companionGender = "neutral",
                    hasNamedCompanion = false
                )
            )

            // Seed initial lessons
            database.lessonDao().insertLesson(
                Lesson(
                    title = "Bilan & Présentation",
                    type = "vocab",
                    subject = "Faire connaissance en anglais",
                    durationMinutes = 10,
                    summary = "Introduction orale, présentation des objectifs et repères de prononciation.",
                    wordsLearned = 5,
                    errorsCount = 1,
                    score = 95,
                    isCompleted = true
                )
            )
            database.lessonDao().insertLesson(
                Lesson(
                    title = "Le Present Perfect en situation",
                    type = "grammar",
                    subject = "Parler de ses expériences",
                    durationMinutes = 15,
                    summary = "Comment exprimer ce qu'on a déjà fait sans confondre avec le prétérit.",
                    wordsLearned = 7,
                    errorsCount = 2,
                    score = 90,
                    isCompleted = true
                )
            )
            database.lessonDao().insertLesson(
                Lesson(
                    title = "Commander au Restaurant",
                    type = "roleplay",
                    subject = "Jeu de rôle à Londres",
                    durationMinutes = 12,
                    summary = "Passer commande, poser des questions sur le menu et demander poliment l'addition.",
                    wordsLearned = 6,
                    errorsCount = 0,
                    score = 98,
                    isCompleted = false
                )
            )

            // Seed vocabulary
            val now = System.currentTimeMillis()
            database.vocabularyDao().insertWord(
                Vocabulary(
                    word = "Fluency",
                    translation = "Aisance / fluidité",
                    example = "She speaks English with natural fluency.",
                    masteryLevel = 3,
                    dateLearned = now - 86400000L * 2
                )
            )
            database.vocabularyDao().insertWord(
                Vocabulary(
                    word = "Resilience",
                    translation = "Résilience",
                    example = "Resilience is essential to build lasting habits.",
                    masteryLevel = 4,
                    dateLearned = now - 86400000L * 3
                )
            )
            database.vocabularyDao().insertWord(
                Vocabulary(
                    word = "Mindset",
                    translation = "État d'esprit",
                    example = "A growth mindset helps you embrace mistakes as learning opportunities.",
                    masteryLevel = 4,
                    dateLearned = now - 86400000L * 4
                )
            )
            database.vocabularyDao().insertWord(
                Vocabulary(
                    word = "Milestone",
                    translation = "Étape clé",
                    example = "Reaching the B2 level is an important milestone.",
                    masteryLevel = 2,
                    dateLearned = now - 86400000L
                )
            )
            database.vocabularyDao().insertWord(
                Vocabulary(
                    word = "Overcome",
                    translation = "Surmonter",
                    example = "With daily practice, you will overcome your fear of public speaking.",
                    masteryLevel = 3,
                    dateLearned = now
                )
            )

            // Seed sample errors for progression analysis
            database.learnedErrorDao().insertError(
                LearnedError(
                    badPhrase = "I am agree with you",
                    correction = "I agree with you",
                    errorType = "Grammaire",
                    count = 3
                )
            )
            database.learnedErrorDao().insertError(
                LearnedError(
                    badPhrase = "She don't like it",
                    correction = "She doesn't like it",
                    errorType = "Temps verbal",
                    count = 2
                )
            )
            database.learnedErrorDao().insertError(
                LearnedError(
                    badPhrase = "Depend of the weather",
                    correction = "Depends on the weather",
                    errorType = "Préposition",
                    count = 2
                )
            )

            // Seed downloadable resource packs
            database.resourceDao().insertResource(
                ResourcePack(
                    id = "model_offline_nano",
                    name = "Modèle IA Léger (On-Device Nano)",
                    type = "Modèle IA Embarqué",
                    sizeMb = 48.0,
                    downloadDate = now - 86400000L * 5,
                    lastUsedDate = now,
                    isDownloaded = true,
                    isProtected = false
                )
            )
            database.resourceDao().insertResource(
                ResourcePack(
                    id = "dict_fr_en",
                    name = "Dictionnaire Anglais-Français Essentiel",
                    type = "Dictionnaire & Traduction",
                    sizeMb = 18.5,
                    downloadDate = now - 86400000L * 5,
                    lastUsedDate = now,
                    isDownloaded = true,
                    isProtected = true // Protected: core progression
                )
            )
            database.resourceDao().insertResource(
                ResourcePack(
                    id = "pack_travel_vocab",
                    name = "Pack Vocabulaire Voyage & Situations réelles",
                    type = "Vocabulaire thématique",
                    sizeMb = 14.2,
                    downloadDate = now - 86400000L * 2,
                    lastUsedDate = now - 86400000L,
                    isDownloaded = true,
                    isProtected = false
                )
            )
            database.resourceDao().insertResource(
                ResourcePack(
                    id = "pack_grammar_rules",
                    name = "Règles de Grammaire & Modaux avancés",
                    type = "Guide pédagogique",
                    sizeMb = 6.0,
                    downloadDate = now - 86400000L * 4,
                    lastUsedDate = now,
                    isDownloaded = true,
                    isProtected = true
                )
            )
            database.resourceDao().insertResource(
                ResourcePack(
                    id = "pack_hd_audio_voices",
                    name = "Pack Voix HD Synthétiseur Audio",
                    type = "Audio & Prononciation",
                    sizeMb = 32.0,
                    downloadDate = 0L,
                    lastUsedDate = 0L,
                    isDownloaded = false,
                    isProtected = false
                )
            )

            // Seed initial downloaded knowledge topics for offline learning and discussion
            database.knowledgeDao().insertKnowledge(
                DownloadedKnowledge(
                    topic = "Les Trous Noirs & l'Astrophysique",
                    category = "Sciences & Univers",
                    summary = "Comprendre les trous noirs, l'horizon des événements, et la relativité générale.",
                    detailedContent = "Un trou noir est une région de l'espace-temps dont le champ gravitationnel est si intense que rien, pas même la lumière, ne peut s'en échapper. Au cœur se trouve une singularité gravitationnelle. La frontière est appelée l'horizon des événements. Les trous noirs stellaires naissent de l'effondrement d'étoiles massives, tandis que des trous noirs supermassifs trônent au centre des galaxies, comme Sagittarius A* dans la Voie Lactée.",
                    keyPoints = "Horizon des événements; Singularité; Relativité générale d'Einstein; Déformation du temps; Trous noirs supermassifs",
                    sampleQuestions = "Comment se forme un trou noir ?; Qu'est-ce que l'horizon des événements ?; Que devient le temps à proximité ?",
                    sizeMb = 2.1,
                    isAvailableOffline = true
                )
            )
            database.knowledgeDao().insertKnowledge(
                DownloadedKnowledge(
                    topic = "L'Empire Romain : De César à la Chute",
                    category = "Histoire & Civilisations",
                    summary = "L'ascension de Rome, l'organisation de l'Empire et les causes de sa chute.",
                    detailedContent = "Fondée selon la légende en 753 avant J.-C., Rome est passée de la royauté à la République, puis à l'Empire sous Auguste en 27 avant J.-C. Les Romains ont unifié le pourtour méditerranéen grâce à leurs légions, leur droit et leurs infrastructures exceptionnelles (aqueducs, voies romaines). La Pax Romana a duré deux siècles, avant les divisions en Empire d'Occident et d'Orient et la chute de Rome en 476.",
                    keyPoints = "République vs Empire; Jules César et Auguste; Pax Romana; Voies romaines et droit civil; Chute en 476",
                    sampleQuestions = "Qui était le premier empereur romain ?; Pourquoi Rome était-elle si puissante militairement ?; Quelles sont les causes de la fin de l'Empire ?",
                    sizeMb = 2.4,
                    isAvailableOffline = true
                )
            )
            database.knowledgeDao().insertKnowledge(
                DownloadedKnowledge(
                    topic = "Les Biais Cognitifs & la Prise de Décision",
                    category = "Psychologie & Esprit",
                    summary = "Découvrir les pièges de notre cerveau et comment penser plus rationnellement.",
                    detailedContent = "Notre cerveau utilise des raccourcis mentaux appelés heuristiques. Ces raccourcis créent des biais cognitifs systématiques : le biais de confirmation (chercher ce qui confirme nos croyances), l'effet d'ancrage (se focaliser sur la première information reçue), et l'aversion à la perte (la douleur de perdre est deux fois plus forte que le plaisir de gagner). Reconnaître ces biais permet de prendre de bien meilleures décisions au travail et dans la vie.",
                    keyPoints = "Biais de confirmation; Effet de halo; Ancrage mental; Aversion à la perte; Pensée Système 1 et Système 2",
                    sampleQuestions = "Qu'est-ce que le biais de confirmation ?; Pourquoi a-t-on peur de perdre ?; Comment déjouer les pièges de son cerveau ?",
                    sizeMb = 1.9,
                    isAvailableOffline = true
                )
            )

            // Seed initial reference documents
            database.documentDao().insertDocument(
                DownloadedDocument(
                    title = "Manuel Pratique d'Anglais Professionnel & Fluency",
                    authorOrSource = "Synthèse Pédagogique Code",
                    topicCategory = "Langues & Carrière",
                    summary = "Expressions clés, tournures professionnelles pour les réunions et négociations en anglais.",
                    fullTextContent = "Pour s'exprimer avec fluidité en anglais professionnel, privilégiez les tournures diplomatiques. Par exemple, au lieu de 'I disagree', utilisez 'I see your point, however I would suggest...'. Pour présenter des résultats, structurez vos interventions en trois temps : contexte, défi résolu, métrique d'impact. Maîtrisez les modaux de politesse : could, would et might.",
                    keywords = "Anglais; Réunions; Négociation; Modaux; Fluency; Vocabulaire",
                    sizeMb = 3.4,
                    isAvailableOffline = true
                )
            )
            database.documentDao().insertDocument(
                DownloadedDocument(
                    title = "Dossier : Origine et Évolution de l'Univers",
                    authorOrSource = "Encyclopédie Scientifique Code",
                    topicCategory = "Sciences & Astronomie",
                    summary = "Du Big Bang à l'expansion accélérée et à la formation des premières galaxies.",
                    fullTextContent = "L'Univers observable s'est formé il y a environ 13,8 milliards d'années à partir d'un état extrêmement dense et chaud. L'inflation cosmique a suivi, puis le rayonnement fossile s'est libéré 380 000 ans plus tard. Aujourd'hui, l'expansion de l'univers s'accélère sous l'effet de l'énergie noire, qui représente environ 68% de la densité d'énergie totale.",
                    keywords = "Univers; Big Bang; Énergie noire; Matière noire; Galaxies; Astrophysique",
                    sizeMb = 4.1,
                    isAvailableOffline = true
                )
            )

            // Seed initial greeting message
            database.conversationDao().insertMessage(
                ConversationMessage(
                    role = "code",
                    text = "Bonjour ! Je suis Code, ton professeur personnel. Je suis prêt à t'accompagner. De quoi aimerais-tu parler aujourd'hui ?",
                    mode = "ENGLISH"
                )
            )
        }
    }
}
