package com.example.data.local

import com.example.data.model.CachedLesson
import org.json.JSONArray
import org.json.JSONObject

object DefaultCachedLessons {

    fun getDefaultLessons(): List<CachedLesson> {
        val now = System.currentTimeMillis()
        return listOf(
            // ==================== ENGLISH LESSONS ====================
            CachedLesson(
                id = "en_intro_a1",
                languageCode = "en",
                languageName = "English",
                title = "First Greetings & Introductions",
                subtitle = "Master essential polite greetings and self-introductions in English",
                level = "A1",
                category = "Conversation",
                durationMinutes = 8,
                summary = "Learn how to greet someone politely, state your name, where you come from, and respond warmly.",
                detailedContent = """
                    In English, greetings set the tone of your conversation. 
                    • Formal greetings: "Good morning", "Good afternoon", "How do you do?"
                    • Friendly & everyday greetings: "Hello!", "Hi there!", "Nice to meet you."
                    • To share your origin: "I come from Paris" or "I am from France."
                    • When asked "How are you?", a natural reply is: "I'm doing well, thank you! How about yourself?"
                """.trimIndent(),
                dialoguesJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("speaker", "Sarah")
                        put("text", "Hello! Nice to meet you. My name is Sarah.")
                        put("translation", "Bonjour ! Ravie de vous rencontrer. Je m'appelle Sarah.")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Alex")
                        put("text", "Hi Sarah! Nice to meet you too. I am Alex from Paris.")
                        put("translation", "Salut Sarah ! Ravi de te rencontrer aussi. Je suis Alex de Paris.")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Sarah")
                        put("text", "Welcome to London! How are you enjoying your trip so far?")
                        put("translation", "Bienvenue à Londres ! Comment se passe ton séjour jusqu'ici ?")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Alex")
                        put("text", "It is wonderful! The city is beautiful and people are very welcoming.")
                        put("translation", "C'est formidable ! La ville est magnifique et les gens sont très accueillants.")
                    })
                }.toString(),
                vocabularyJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("word", "Pleasure")
                        put("phonetic", "/ˈpleʒ.ər/")
                        put("translation", "Plaisir")
                        put("example", "It is a pleasure to meet you.")
                    })
                    put(JSONObject().apply {
                        put("word", "Welcome")
                        put("phonetic", "/ˈwel.kəm/")
                        put("translation", "Bienvenue")
                        put("example", "Welcome to our team!")
                    })
                    put(JSONObject().apply {
                        put("word", "Wonderful")
                        put("phonetic", "/ˈwʌn.də.fəl/")
                        put("translation", "Formidable / Merveilleux")
                        put("example", "We had a wonderful morning.")
                    })
                    put(JSONObject().apply {
                        put("word", "Welcoming")
                        put("phonetic", "/ˈwel.kəm.ɪŋ/")
                        put("translation", "Accueillant")
                        put("example", "The locals are extremely welcoming.")
                    })
                }.toString(),
                exerciseQuizJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("question", "What is the most natural reply to 'Nice to meet you'?")
                        put("options", JSONArray(listOf("Nice to meet you too!", "Yesterday morning", "I am agree", "Good evening no")))
                        put("correctIndex", 0)
                        put("explanation", "'Nice to meet you too!' is the polite standard response.")
                    })
                    put(JSONObject().apply {
                        put("question", "Which question asks about someone's origin?")
                        put("options", JSONArray(listOf("What time is it?", "Where are you from?", "How old are you?", "What do you do?")))
                        put("correctIndex", 1)
                        put("explanation", "'Where are you from?' inquires about where a person comes from.")
                    })
                }.toString(),
                isCachedOffline = true,
                lastUpdated = now
            ),

            CachedLesson(
                id = "en_cafe_a2",
                languageCode = "en",
                languageName = "English",
                title = "Ordering in a British Café",
                subtitle = "Politely order coffee, pastries, and handle card payments in London",
                level = "A2",
                category = "Voyage & Quotidien",
                durationMinutes = 10,
                summary = "Practice modal verbs (could, would) to ask for items politely and navigate café interactions.",
                detailedContent = """
                    In the UK and US, direct commands sound impolite. Use modal verbs:
                    • Instead of "Give me a coffee", use "Could I please have a cappuccino?"
                    • Key question: "For here or to take away?" (UK) or "For here or to go?" (US)
                    • Payment: "Can I pay by card?" / "Is contactless accepted?"
                """.trimIndent(),
                dialoguesJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("speaker", "Barista")
                        put("text", "Hi there! What can I get started for you today?")
                        put("translation", "Bonjour ! Que puis-je vous servir aujourd'hui ?")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Customer")
                        put("text", "Could I have an oat milk cappuccino and an almond croissant, please?")
                        put("translation", "Pourrais-je avoir un cappuccino au lait d'avoine et un croissant aux amandes, s'il vous plaît ?")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Barista")
                        put("text", "Certainly! For here or to take away?")
                        put("translation", "Certainement ! Sur place ou à emporter ?")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Customer")
                        put("text", "To take away, please. Can I pay with contactless?")
                        put("translation", "À emporter, s'il vous plaît. Puis-je payer en sans contact ?")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Barista")
                        put("text", "Yes, just tap right here. That will be four pounds fifty.")
                        put("translation", "Oui, posez simplement votre carte ici. Cela fera quatre livres cinquante.")
                    })
                }.toString(),
                vocabularyJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("word", "Take away")
                        put("phonetic", "/ˈteɪk.ə.weɪ/")
                        put("translation", "À emporter")
                        put("example", "Two coffees to take away, please.")
                    })
                    put(JSONObject().apply {
                        put("word", "Contactless")
                        put("phonetic", "/ˈkɒn.tækt.ləs/")
                        put("translation", "Sans contact")
                        put("example", "Contactless payment is available.")
                    })
                    put(JSONObject().apply {
                        put("word", "Receipt")
                        put("phonetic", "/rɪˈsiːt/")
                        put("translation", "Ticket de caisse / reçu")
                        put("example", "Would you like your receipt?")
                    })
                    put(JSONObject().apply {
                        put("word", "Certainly")
                        put("phonetic", "/ˈsɜː.tən.li/")
                        put("translation", "Certainement / avec plaisir")
                        put("example", "Certainly, right away!")
                    })
                }.toString(),
                exerciseQuizJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("question", "What is the polite way to ask for an item in a café?")
                        put("options", JSONArray(listOf("Could I have a coffee, please?", "Give me coffee!", "I want coffee now", "Coffee here")))
                        put("correctIndex", 0)
                        put("explanation", "'Could I have..., please?' is the polite formulation.")
                    })
                    put(JSONObject().apply {
                        put("question", "What does 'To take away' mean in British English?")
                        put("options", JSONArray(listOf("Sur place", "À emporter", "En terrasse", "Chauffé au micro-ondes")))
                        put("correctIndex", 1)
                        put("explanation", "'To take away' (or 'to go' in the US) means taking food outside the shop.")
                    })
                }.toString(),
                isCachedOffline = true,
                lastUpdated = now
            ),

            CachedLesson(
                id = "en_travel_b1",
                languageCode = "en",
                languageName = "English",
                title = "Travel Memories: Past Simple vs Present Perfect",
                subtitle = "Express past adventures and lifelong experiences with confidence",
                level = "B1",
                category = "Grammaire & Récit",
                durationMinutes = 12,
                summary = "Differentiate between finished past events (Past Simple) and experiences connected to the present (Present Perfect).",
                detailedContent = """
                    A classic dilemma for language learners:
                    • Present Perfect (have + past participle): for life experiences with no specific past date.
                      "I have visited London twice." / "Have you ever seen the Northern Lights?"
                    • Past Simple: for actions completed at a specific time in the past.
                      "Last year, I went to Edinburgh and saw the castle."
                """.trimIndent(),
                dialoguesJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("speaker", "Emma")
                        put("text", "Have you ever travelled to Scotland, Alex?")
                        put("translation", "As-tu déjà voyagé en Écosse, Alex ?")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Alex")
                        put("text", "Yes, I have! I visited the Highlands two years ago.")
                        put("translation", "Oui, tout à fait ! J'ai visité les Highlands il y a deux ans.")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Emma")
                        put("text", "Did you enjoy hiking around the lochs?")
                        put("translation", "As-tu aimé randonner autour des lochs ?")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Alex")
                        put("text", "It was unforgettable! The landscapes were breathtaking.")
                        put("translation", "C'était inoubliable ! Les paysages étaient à couper le souffle.")
                    })
                }.toString(),
                vocabularyJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("word", "Breathtaking")
                        put("phonetic", "/ˈbreθˌteɪ.kɪŋ/")
                        put("translation", "À couper le souffle")
                        put("example", "The mountain view was breathtaking.")
                    })
                    put(JSONObject().apply {
                        put("word", "Unforgettable")
                        put("phonetic", "/ˌʌn.fəˈɡet.ə.bəl/")
                        put("translation", "Inoubliable")
                        put("example", "Our road trip was an unforgettable experience.")
                    })
                    put(JSONObject().apply {
                        put("word", "Landscape")
                        put("phonetic", "/ˈlænd.skeɪp/")
                        put("translation", "Paysage")
                        put("example", "The Scottish landscape is rugged and green.")
                    })
                    put(JSONObject().apply {
                        put("word", "Hiking")
                        put("phonetic", "/ˈhaɪ.kɪŋ/")
                        put("translation", "Randonnée")
                        put("example", "We went hiking in the national park.")
                    })
                }.toString(),
                exerciseQuizJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("question", "Complete: 'I _____ to Japan in 2022.'")
                        put("options", JSONArray(listOf("have gone", "went", "was go", "am going")))
                        put("correctIndex", 1)
                        put("explanation", "With a specific past date ('in 2022'), use the Past Simple: 'went'.")
                    })
                    put(JSONObject().apply {
                        put("question", "Which sentence expresses a life experience without a specific date?")
                        put("options", JSONArray(listOf("I have seen that movie.", "I saw that movie yesterday.", "I was seeing that movie.", "I see that movie now.")))
                        put("correctIndex", 0)
                        put("explanation", "'I have seen that movie' uses Present Perfect to denote life experience.")
                    })
                }.toString(),
                isCachedOffline = true,
                lastUpdated = now
            ),

            CachedLesson(
                id = "en_business_b2",
                languageCode = "en",
                languageName = "English",
                title = "Pitching an Innovative Project",
                subtitle = "Present ideas persuasively, handle feedback, and debate during tech meetings",
                level = "B2",
                category = "Business & Débat",
                durationMinutes = 15,
                summary = "Develop business communication skills: value propositions, handling objections, and collaborative discussions.",
                detailedContent = """
                    Key techniques for business presentations in English:
                    • Structuring the pitch: "First, let's look at the problem...", "Secondly, our core value proposition...", "Finally, the measurable impact..."
                    • Handling counter-arguments: "That's a valid point, however...", "To address your concern, we have designed..."
                    • Softening statements: "It might be worth considering...", "We could potentially optimize..."
                """.trimIndent(),
                dialoguesJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("speaker", "Lead Engineer")
                        put("text", "Alex, could you present how the on-device AI architecture improves privacy?")
                        put("translation", "Alex, pourrais-tu présenter comment l'architecture IA sur l'appareil améliore la confidentialité ?")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Alex")
                        put("text", "Certainly. By processing voice directly on the device with local neural embeddings, user data never leaves the handset, cutting latency by 60%.")
                        put("translation", "Certainement. En traitant la voix directement sur l'appareil avec des embeddings neuronaux locaux, les données utilisateur ne quittent jamais le combiné, réduisant la latence de 60%.")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Product Manager")
                        put("text", "That sounds compelling. What about memory footprint and battery efficiency?")
                        put("translation", "C'est très convaincant. Qu'en est-il de l'empreinte mémoire et de l'efficacité énergétique ?")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Alex")
                        put("text", "We leveraged quantized 4-bit weights and Room DB caching to keep RAM usage well under 120 megabytes.")
                        put("translation", "Nous avons utilisé des poids quantifiés en 4 bits et la mise en cache Room pour maintenir l'empreinte RAM bien sous les 120 mégaoctets.")
                    })
                }.toString(),
                vocabularyJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("word", "Compelling")
                        put("phonetic", "/kəmˈpel.ɪŋ/")
                        put("translation", "Convaincant / Captivant")
                        put("example", "She gave a compelling presentation.")
                    })
                    put(JSONObject().apply {
                        put("word", "Footprint")
                        put("phonetic", "/ˈfʊt.prɪnt/")
                        put("translation", "Empreinte (mémoire / écologique)")
                        put("example", "The memory footprint was minimized.")
                    })
                    put(JSONObject().apply {
                        put("word", "Leverage")
                        put("phonetic", "/ˈlev.ər.ɪdʒ/")
                        put("translation", "Exploiter / Mettre à profit")
                        put("example", "We leverage modern database caching.")
                    })
                    put(JSONObject().apply {
                        put("word", "Latency")
                        put("phonetic", "/ˈleɪ.tən.si/")
                        put("translation", "Latence / Temps de réponse")
                        put("example", "Local processing eliminates network latency.")
                    })
                }.toString(),
                exerciseQuizJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("question", "What is the meaning of 'compelling' in a business presentation?")
                        put("options", JSONArray(listOf("Convincing and powerful", "Boring and long", "Expensive", "Dangerous")))
                        put("correctIndex", 0)
                        put("explanation", "'Compelling' means highly convincing, persuasive, and attractive.")
                    })
                    put(JSONObject().apply {
                        put("question", "How can you diplomatically answer a stakeholder objection?")
                        put("options", JSONArray(listOf("You are totally wrong", "That is a valid point, however...", "I do not care", "Whatever you say")))
                        put("correctIndex", 1)
                        put("explanation", "'That is a valid point, however...' validates the interlocutor while presenting alternative evidence.")
                    })
                }.toString(),
                isCachedOffline = true,
                lastUpdated = now
            ),

            // ==================== FRENCH LESSONS (FRANÇAIS) ====================
            CachedLesson(
                id = "fr_intro_a1",
                languageCode = "fr",
                languageName = "Français",
                title = "Se présenter et saluer en français",
                subtitle = "Les bases d'un premier contact chaleureux et naturel en français",
                level = "A1",
                category = "Conversation",
                durationMinutes = 8,
                summary = "Apprendre les formules de politesse indispensables, dire son nom, sa nationalité et demander des nouvelles.",
                detailedContent = """
                    En français, la politesse commence par un bonjour clair et souriant :
                    • Selon le moment de la journée : « Bonjour » (journée) ou « Bonsoir » (après 18h).
                    • Pour se présenter : « Je m'appelle Alex » ou « Je suis Alex, ravi de faire votre connaissance ».
                    • Le tutoiement (« tu ») et le vouvoiement (« vous ») : utilise « vous » avec des inconnus ou dans un cadre formel, et « tu » avec des amis ou collègues proches.
                """.trimIndent(),
                dialoguesJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("speaker", "Camille")
                        put("text", "Bonjour ! Bienvenue à Paris. Comment vous appelez-vous ?")
                        put("translation", "Hello! Welcome to Paris. What is your name?")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Alex")
                        put("text", "Bonjour Camille ! Je m'appelle Alex. Enchanté de faire votre connaissance.")
                        put("translation", "Hello Camille! My name is Alex. Delighted to meet you.")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Camille")
                        put("text", "Enchantée Alex ! Comment se passe votre arrivée en France ?")
                        put("translation", "Nice to meet you Alex! How is your arrival in France going?")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Alex")
                        put("text", "Tout se passe à merveille, merci beaucoup ! Les gens sont très bienveillants.")
                        put("translation", "Everything is going wonderfully, thank you so much! People are very kind.")
                    })
                }.toString(),
                vocabularyJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("word", "Enchanté")
                        put("phonetic", "/ɑ̃.ʃɑ̃.te/")
                        put("translation", "Delighted / Nice to meet you")
                        put("example", "Enchanté de vous rencontrer !")
                    })
                    put(JSONObject().apply {
                        put("word", "Bienveillant")
                        put("phonetic", "/bjɛ̃.vɛ.jɑ̃/")
                        put("translation", "Benevolent / Caring / Kind")
                        put("example", "Un accueil chaleureux et bienveillant.")
                    })
                    put(JSONObject().apply {
                        put("word", "Merveille")
                        put("phonetic", "/mɛʁ.vɛj/")
                        put("translation", "Wonder / Marvel")
                        put("example", "Tout s'est déroulé à merveille.")
                    })
                    put(JSONObject().apply {
                        put("word", "Politesse")
                        put("phonetic", "/pɔ.li.tɛs/")
                        put("translation", "Politeness / Courtesy")
                        put("example", "La politesse ouvre toutes les portes.")
                    })
                }.toString(),
                exerciseQuizJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("question", "Que dit-on poliment en français après s'être présenté à quelqu'un ?")
                        put("options", JSONArray(listOf("Enchanté(e) !", "Au revoir !", "De rien !", "Tant pis !")))
                        put("correctIndex", 0)
                        put("explanation", "« Enchanté(e) » est la formule classique marquant le plaisir d'une rencontre.")
                    })
                    put(JSONObject().apply {
                        put("question", "À partir de quelle heure dit-on généralement 'Bonsoir' ?")
                        put("options", JSONArray(listOf("En fin d'après-midi (vers 18h)", "À midi", "À 8h du matin", "Jamais")))
                        put("correctIndex", 0)
                        put("explanation", "En français, on passe de « Bonjour » à « Bonsoir » aux alentours de 18h.")
                    })
                }.toString(),
                isCachedOffline = true,
                lastUpdated = now
            ),

            CachedLesson(
                id = "fr_boulangerie_a2",
                languageCode = "fr",
                languageName = "Français",
                title = "À la Boulangerie & Dans la Rue",
                subtitle = "Commander du pain frais, des viennoiseries et demander son chemin",
                level = "A2",
                category = "Vie quotidienne",
                durationMinutes = 10,
                summary = "Mémoriser les tournures indispensables pour faire ses courses chez les commerçants de quartier et s'orienter en ville.",
                detailedContent = """
                    La boulangerie est le cœur battant du quotidien en France :
                    • La commande polie : « Bonjour madame, je voudrais une baguette tradition pas trop cuite, s'il vous plaît. »
                    • Les viennoiseries : croissant au beurre, pain au chocolat (ou chocolatine !), chausson aux pommes.
                    • La question du commerçant : « Et avec ceci ? » ou « Ce sera tout ? »
                    • La réponse : « Ce sera tout, merci ! Combien je vous dois ? »
                """.trimIndent(),
                dialoguesJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("speaker", "Boulanger")
                        put("text", "Bonjour monsieur ! Qu'est-ce qui vous ferait plaisir aujourd'hui ?")
                        put("translation", "Hello sir! What can I get for you today?")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Client")
                        put("text", "Bonjour ! Je voudrais une baguette tradition bien dorée et deux croissants, s'il vous plaît.")
                        put("translation", "Hello! I would like a golden traditional baguette and two croissants, please.")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Boulanger")
                        put("text", "Très bien ! Et avec ceci ?")
                        put("translation", "Very good! And anything else with that?")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Client")
                        put("text", "Ce sera tout, merci. Est-ce que vous prenez la carte bancaire ?")
                        put("translation", "That will be all, thank you. Do you accept credit cards?")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Boulanger")
                        put("text", "Oui, sans contact dès un euro. Ça vous fera trois euros soixante.")
                        put("translation", "Yes, contactless from one euro. That will be three euros sixty.")
                    })
                }.toString(),
                vocabularyJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("word", "Tradition")
                        put("phonetic", "/tʁa.di.sjɔ̃/")
                        put("translation", "Baguette tradition (pain artisanal)")
                        put("example", "Une tradition bien cuite, s'il vous plaît.")
                    })
                    put(JSONObject().apply {
                        put("word", "Viennoiserie")
                        put("phonetic", "/vjɛ.nwa.zʁi/")
                        put("translation", "Pastry (croissants, brioches...)")
                        put("example", "Les viennoiseries sortent du four.")
                    })
                    put(JSONObject().apply {
                        put("word", "Monnaie")
                        put("phonetic", "/mɔ.nɛ/")
                        put("translation", "Change / Coins")
                        put("example", "Avez-vous de la monnaie sur dix euros ?")
                    })
                    put(JSONObject().apply {
                        put("word", "Doré")
                        put("phonetic", "/dɔ.ʁe/")
                        put("translation", "Golden / Nicely baked")
                        put("example", "Un croissant bien doré et croustillant.")
                    })
                }.toString(),
                exerciseQuizJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("question", "Que signifie la question du commerçant : 'Et avec ceci ?' ?")
                        put("options", JSONArray(listOf("Désirez-vous autre chose ?", "Quel est votre nom ?", "Payez-vous par carte ?", "Quelle heure est-il ?")))
                        put("correctIndex", 0)
                        put("explanation", "« Et avec ceci ? » est la formule usuelle pour demander si vous souhaitez un autre article.")
                    })
                    put(JSONObject().apply {
                        put("question", "Comment dit-on poliment que l'on a fini sa commande ?")
                        put("options", JSONArray(listOf("Ce sera tout, merci !", "Stop maintenant", "Je refuse", "C'est fini dehors")))
                        put("correctIndex", 0)
                        put("explanation", "« Ce sera tout, merci ! » est la réponse naturelle et courtoise.")
                    })
                }.toString(),
                isCachedOffline = true,
                lastUpdated = now
            ),

            CachedLesson(
                id = "fr_souvenirs_b1",
                languageCode = "fr",
                languageName = "Français",
                title = "Raconter un Souvenir : Passé Composé & Imparfait",
                subtitle = "Maîtriser la narration d'événements passés et la description d'ambiances",
                level = "B1",
                category = "Grammaire & Récit",
                durationMinutes = 12,
                summary = "Comprendre l'articulation entre l'action ponctuelle (passé composé) et le décor / l'habitude (imparfait).",
                detailedContent = """
                    Raconter une histoire au passé en français repose sur une alternance élégante :
                    • L'Imparfait : plante le décor, décrit la météo, les sentiments et les habitudes.
                      Ex : « Il faisait beau, les oiseaux chantaient et nous étions heureux. »
                    • Le Passé Composé : exprime les événements soudains et les actions qui font avancer l'histoire.
                      Ex : « Tout à coup, le train est arrivé et nous sommes montés à bord. »
                """.trimIndent(),
                dialoguesJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("speaker", "Lucas")
                        put("text", "Alex, raconte-moi tes vacances en Provence l'été dernier !")
                        put("translation", "Alex, tell me about your holidays in Provence last summer!")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Alex")
                        put("text", "C'était magnifique. Chaque matin, le soleil brillait et nous nous promenions sur les marchés.")
                        put("translation", "It was magnificent. Every morning, the sun was shining and we walked through markets.")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Lucas")
                        put("text", "Et vous avez visité les champs de lavande ?")
                        put("translation", "And did you visit the lavender fields?")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Alex")
                        put("text", "Oui ! Un après-midi, nous avons loué des vélos et nous avons découvert un champ immense au coucher du soleil.")
                        put("translation", "Yes! One afternoon, we rented bicycles and discovered an immense field at sunset.")
                    })
                }.toString(),
                vocabularyJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("word", "Inoubliable")
                        put("phonetic", "/i.nu.bli.jabl/")
                        put("translation", "Unforgettable")
                        put("example", "Ce coucher de soleil était inoubliable.")
                    })
                    put(JSONObject().apply {
                        put("word", "Atypique")
                        put("phonetic", "/a.ti.pik/")
                        put("translation", "Atypical / Unique")
                        put("example", "Un village provençal au charme atypique.")
                    })
                    put(JSONObject().apply {
                        put("word", "Émerveillement")
                        put("phonetic", "/e.mɛʁ.vɛj.mɑ̃/")
                        put("translation", "Wonder / Amazement")
                        put("example", "Les enfants regardaient le paysage avec émerveillement.")
                    })
                    put(JSONObject().apply {
                        put("word", "Soudain")
                        put("phonetic", "/su.dɛ̃/")
                        put("translation", "Suddenly")
                        put("example", "Soudain, une douce brise s'est levée.")
                    })
                }.toString(),
                exerciseQuizJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("question", "Quel temps utilise-t-on pour décrire la météo dans le passé ?")
                        put("options", JSONArray(listOf("L'imparfait (ex: Il pleuvait)", "Le futur simple", "Le présent de l'indicatif", "Le subjonctif")))
                        put("correctIndex", 0)
                        put("explanation", "L'imparfait sert à décrire le décor et les conditions d'arrière-plan.")
                    })
                    put(JSONObject().apply {
                        put("question", "Complète la phrase : 'Hier, nous _____ un excellent restaurant provençal.'")
                        put("options", JSONArray(listOf("avons découvert", "découvrions", "découvrirons", "découvrons")))
                        put("correctIndex", 0)
                        put("explanation", "Une action ponctuelle et achevée ('hier') requiert le passé composé : 'avons découvert'.")
                    })
                }.toString(),
                isCachedOffline = true,
                lastUpdated = now
            ),

            CachedLesson(
                id = "fr_debat_b2",
                languageCode = "fr",
                languageName = "Français",
                title = "L'Art du Débat & la Nuance en Français",
                subtitle = "Exprimer son opinion, concéder un point et convaincre avec élégance",
                level = "B2",
                category = "Business & Débat",
                durationMinutes = 15,
                summary = "Structurer sa pensée, introduire des connecteurs logiques de concession et affiner son éloquence orale.",
                detailedContent = """
                    Dans la culture francophone, l'art de la discussion repose sur la nuance et la rigueur intellectuelle :
                    • Introduire un avis réfléchi : « À mon sens... », « J'estime que... », « Il me semble judicieux de... »
                    • Exprimer une concession constructive : « Certes..., mais néanmoins... », « Tout en reconnaissant la pertinence de cet argument... »
                    • Conclure avec clarté : « En définitive, il convient de trouver un juste équilibre entre innovation et responsabilité. »
                """.trimIndent(),
                dialoguesJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("speaker", "Sophie")
                        put("text", "Alex, penses-tu que l'intelligence artificielle embarquée remplacera les serveurs distants ?")
                        put("translation", "Alex, do you think on-device artificial intelligence will replace remote servers?")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Alex")
                        put("text", "Certes, les modèles massifs dans le cloud restent irremplaçables pour les calculs lourds, néanmoins le traitement local garantit une souveraineté et une confidentialité indispensables.")
                        put("translation", "Admittedly, massive cloud models remain irreplaceable for heavy computations, nevertheless local processing guarantees essential sovereignty and confidentiality.")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Sophie")
                        put("text", "C'est une observation très juste. L'idéal est donc une approche hybride et agile.")
                        put("translation", "That is a very accurate observation. The ideal is therefore a hybrid and agile approach.")
                    })
                    put(JSONObject().apply {
                        put("speaker", "Alex")
                        put("text", "Absolument. En combinant la mise en cache locale Room et la réactivité vocale, l'utilisateur garde le contrôle total de son apprentissage.")
                        put("translation", "Absolutely. By combining local Room caching and vocal responsiveness, the user maintains complete control over their learning.")
                    })
                }.toString(),
                vocabularyJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("word", "Néanmoins")
                        put("phonetic", "/ne.ɑ̃.mwɛ̃/")
                        put("translation", "Nevertheless / Nonetheless")
                        put("example", "L'exercice est exigeant, néanmoins très formateur.")
                    })
                    put(JSONObject().apply {
                        put("word", "Pertinence")
                        put("phonetic", "/pɛʁ.ti.nɑ̃s/")
                        put("translation", "Relevance / Pertinence")
                        put("example", "La pertinence de cette analyse ne fait aucun doute.")
                    })
                    put(JSONObject().apply {
                        put("word", "Souveraineté")
                        put("phonetic", "/su.vʁɛn.te/")
                        put("translation", "Sovereignty / Autonomy")
                        put("example", "La souveraineté numérique protège la vie privée.")
                    })
                    put(JSONObject().apply {
                        put("word", "Inhérent")
                        put("phonetic", "/i.ne.ʁɑ̃/")
                        put("translation", "Inherent")
                        put("example", "Les défis inhérents à l'apprentissage d'une langue.")
                    })
                }.toString(),
                exerciseQuizJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("question", "Quel connecteur logique permet d'exprimer une concession élégante en français ?")
                        put("options", JSONArray(listOf("Certes..., néanmoins...", "Parce que", "D'abord puis", "Donc")))
                        put("correctIndex", 0)
                        put("explanation", "« Certes..., néanmoins... » permet d'admettre un point tout en apportant une nuance déterminante.")
                    })
                    put(JSONObject().apply {
                        put("question", "Quelle formule exprime une prise de position personnelle modérée ?")
                        put("options", JSONArray(listOf("À mon sens...", "Tout le monde doit penser comme moi", "Tu as tort", "C'est faux")))
                        put("correctIndex", 0)
                        put("explanation", "« À mon sens... » ou « Il me semble que... » introduit son point de vue avec finesse.")
                    })
                }.toString(),
                isCachedOffline = true,
                lastUpdated = now
            )
        )
    }
}
