package com.example.data.datasource

import com.example.data.model.AlarmToneItem
import com.example.data.model.DhikrPreset
import com.example.data.model.DuaItem
import com.example.data.model.LocationConfig

object IslamicData {

    val ALARM_TONES: List<AlarmToneItem> = listOf(
        AlarmToneItem("alarm_tone_1", "Allahu Akbar - Makkah Azan", "اذانِ مکہ مکرمہ 🕋", "alarm_tone_1"),
        AlarmToneItem("alarm_tone_2", "Classic Alert Beep", "کلاسک بیپ الارم ⏰", "alarm_tone_2"),
        AlarmToneItem("alarm_tone_3", "Gentle Morning Chime", "صبح کی پرسکون دھن 🌅", "alarm_tone_3"),
        AlarmToneItem("alarm_tone_4", "Spiritual Serenity Melody", "روحانی سرور و نے 🎵", "alarm_tone_4"),
        AlarmToneItem("alarm_tone_5", "Fajr Special (As-Salatu Khayrun)", "الصلوٰۃ خیر من النوم 🕌", "alarm_tone_5"),
        AlarmToneItem("alarm_tone_6", "Salli Ala Muhammad Nasheed", "صَلِّ عَلَىٰ مُحَمَّد نعت ✨", "alarm_tone_6"),
        AlarmToneItem("alarm_tone_7", "Habibi Ya RasoolAllah", "حبیبی یا رسول اللہ 🌹", "alarm_tone_7"),
        AlarmToneItem("alarm_tone_8", "Allahu Akbar Melodic Adhan", "اللہ اکبر نغمہ و اذان 🎶", "alarm_tone_8"),
        AlarmToneItem("alarm_tone_9", "Peaceful Islamic Tone", "روحانی سکون نغمہ 🌿", "alarm_tone_9"),
        AlarmToneItem("alarm_tone_10", "SubhanAllah Wal-Hamdulillah", "تسبیح و حمد الٰہی 📿", "alarm_tone_10"),
        AlarmToneItem("alarm_tone_11", "Ya Nabi Salam Alayka", "یا نبی سلام علیک 💫", "alarm_tone_11"),
        AlarmToneItem("custom_tone", "Custom Audio from Device", "ڈیوائس سے اپنی پسندیدہ آڈیو منتخب کریں 📁", null, isCustom = true)
    )

    val DHIKR_PRESETS: List<DhikrPreset> = listOf(
        DhikrPreset(
            id = "subhanallah",
            arabic = "سُبْحَانَ اللَّهِ",
            transliteration = "SubhanAllah",
            urdu = "اللہ پاک ہے",
            meaning = "Glory be to Allah",
            defaultGoal = 33
        ),
        DhikrPreset(
            id = "alhamdulillah",
            arabic = "الْحَمْدُ لِلَّهِ",
            transliteration = "Alhamdulillah",
            urdu = "تمام تعریفیں اللہ کے لیے ہیں",
            meaning = "All praise is due to Allah",
            defaultGoal = 33
        ),
        DhikrPreset(
            id = "allahuakbar",
            arabic = "اللَّهُ أَكْبَرُ",
            transliteration = "Allahu Akbar",
            urdu = "اللہ سب سے بڑا ہے",
            meaning = "Allah is the Greatest",
            defaultGoal = 34
        ),
        DhikrPreset(
            id = "astaghfirullah",
            arabic = "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ",
            transliteration = "Astaghfirullah wa atubu ilayh",
            urdu = "میں اللہ سے بخشش مانگتا ہوں اور اسی کی طرف رجوع کرتا ہوں",
            meaning = "I seek forgiveness of Allah and turn to Him in repentance",
            defaultGoal = 100
        ),
        DhikrPreset(
            id = "lailahaillallah",
            arabic = "لَا إِلٰهَ إِلَّا اللَّهُ مُحَمَّدٌ رَسُولُ اللَّهِ",
            transliteration = "La ilaha illallah Muhammadur Rasulullah",
            urdu = "اللہ کے سوا کوئی معبود نہیں، محمد اللہ کے رسول ہیں",
            meaning = "None has the right to be worshipped except Allah, Muhammad is His Messenger",
            defaultGoal = 100
        ),
        DhikrPreset(
            id = "durood",
            arabic = "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ",
            transliteration = "Allahumma salli ala Muhammadin wa ala aali Muhammad",
            urdu = "اے اللہ! رحمت نازل فرما محمد پر اور ان کی آل پر",
            meaning = "O Allah, bestow peace upon Muhammad and the family of Muhammad",
            defaultGoal = 100
        ),
        DhikrPreset(
            id = "subhanallah_bihamdihi",
            arabic = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            transliteration = "SubhanAllahi wa bihamdihi, SubhanAllahil Azeem",
            urdu = "اللہ پاک ہے اپنی تعریفوں کے ساتھ، اللہ پاک ہے بڑی عظمت والا",
            meaning = "Glory be to Allah and His is the praise, Glory be to Allah the Greatest",
            defaultGoal = 100
        ),
        DhikrPreset(
            id = "la_hawla",
            arabic = "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            transliteration = "La hawla wa la quwwata illa billah",
            urdu = "برائی سے بچنے اور نیکی کرنے کی طاقت اللہ ہی کی توفیق سے ہے",
            meaning = "There is no power and no strength except with Allah",
            defaultGoal = 33
        )
    )

    val MASNOON_DUAS: List<DuaItem> = listOf(
        DuaItem(
            id = "post_salah_istighfar",
            title = "Istighfar after Salah",
            urduTitle = "نماز کے بعد استغفار",
            category = "after_namaz",
            arabic = "أَسْتَغْفِرُ اللَّهَ، أَسْتَغْفِرُ اللَّهَ، أَسْتَغْفِرُ اللَّهَ، اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ",
            transliteration = "Astaghfirullah (3x). Allahumma antas-Salamu wa minkas-Salam, tabarakta ya dhal-Jalali wal-Ikram.",
            urduTranslation = "میں اللہ سے بخشش طلب کرتا ہوں (تین بار)۔ اے اللہ! تو سلامتی والا ہے اور تیری ہی طرف سے سلامتی ہے، تو برکت والا ہے اے عظمت اور بزرگی والے۔",
            translation = "I ask Allah for forgiveness (three times). O Allah, You are Peace and from You comes peace. Blessed are You, O Owner of majesty and honor.",
            reference = "Sahih Muslim 591"
        ),
        DuaItem(
            id = "ayat_al_kursi",
            title = "Ayat al-Kursi (The Throne Verse)",
            urduTitle = "آیۃ الکرسی (نماز کے بعد)",
            category = "after_namaz",
            arabic = "اللَّهُ لَا إِلٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ",
            transliteration = "Allahu la ilaha illa Huwa, Al-Hayyul-Qayyum. La ta'khudhuhu sinatun wa la nawm. Lahu ma fis-samawati wa ma fil-ard...",
            urduTranslation = "اللہ تعالیٰ کے سوا کوئی معبود نہیں، وہ زندہ اور سب کو سنبھالنے والا ہے۔ اسے نہ اونگھ آتی ہے نہ نیند۔ آسمانوں اور زمین کی تمام چیزیں اسی کی ہیں۔",
            translation = "Allah! There is no deity except Him, the Ever-Living, the Sustainer of all existence. Neither drowsiness overtakes Him nor sleep. To Him belongs whatever is in the heavens and whatever is on the earth...",
            reference = "Surah Al-Baqarah 2:255 (Sunan an-Nasa'i 9928)"
        ),
        DuaItem(
            id = "dua_aid_worship",
            title = "Supplication for Remembrance & Gratitude",
            urduTitle = "ذکر، شکر اور حسن عبادت کی دعا",
            category = "after_namaz",
            arabic = "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ",
            transliteration = "Allahumma a'inni 'ala dhikrika wa shukrika wa husni 'ibadatik.",
            urduTranslation = "اے اللہ! اپنے ذکر، اپنے شکر اور اپنی اچھی عبادت پر میری مدد فرما۔",
            translation = "O Allah, help me to remember You, give thanks to You, and worship You in the best way.",
            reference = "Abu Dawud 1522"
        ),
        DuaItem(
            id = "tahajjud_wake",
            title = "Waking up for Tahajjud / Morning",
            urduTitle = "نیند سے بیدار ہونے کی دعا",
            category = "morning_evening",
            arabic = "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
            transliteration = "Alhamdu lillahilladhi ahyana ba'da ma amatana wa ilayhin-nushoor.",
            urduTranslation = "تمام تعریفیں اللہ کے لیے ہیں جس نے ہمیں مارنے کے بعد زندہ کیا اور اسی کی طرف لوٹ کر جانا ہے۔",
            translation = "All praise is for Allah who gave us life after having taken it from us, and unto Him is the resurrection.",
            reference = "Sahih Bukhari 6312"
        ),
        DuaItem(
            id = "parents_dua",
            title = "Supplication for Parents",
            urduTitle = "والدین کے لیے دعا",
            category = "forgiveness",
            arabic = "رَّبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا",
            transliteration = "Rabbir-hamhuma kama rabbayani sagheera.",
            urduTranslation = "اے میرے رب! ان دونوں پر رحم فرما جیسا کہ انہوں نے مجھے بچپن میں پالا۔",
            translation = "My Lord, have mercy upon them as they brought me up when I was small.",
            reference = "Surah Al-Isra 17:24"
        ),
        DuaItem(
            id = "sleep_protection",
            title = "Before Sleeping (Protection)",
            urduTitle = "سوتے وقت کی دعا",
            category = "sleep",
            arabic = "بِاسْمِكَ رَبِّي وَضَعْتُ جَنْبِي وَبِكَ أَرْفَعُهُ، فَإِنْ أَمْسَكْتَ نَفْسِي فَارْحَمْهَا، وَإِنْ أَرْسَلْتَهَا فَاحْفَظْهَا بِمَا تَحْفَظُ بِهِ عِبَادَكَ الصَّالِحِينَ",
            transliteration = "Bismika Rabbi wada'tu janbi wa bika arfa'uh. Fa-in amsakta nafsi far-hamha, wa-in arsaltaha fahfadh-ha bima tahfadhu bihi 'ibadakas-saliheen.",
            urduTranslation = "اے میرے پروردگار! میں نے تیرے نام کے ساتھ اپنا پہلو رکھا اور تیرے ہی نام سے اسے اٹھاؤں گا۔ اگر تو میری جان روک لے تو اس پر رحم فرما اور اگر تو اسے چھوڑ دے تو اس کی حفاظت فرما جس سے تو اپنے نیک بندوں کی حفاظت کرتا ہے۔",
            translation = "In Your name my Lord, I lie down and in Your name I rise. If You should take my soul then have mercy upon it, and if You should return my soul then protect it with that which You protect Your righteous servants.",
            reference = "Sahih Bukhari 6320"
        ),
        DuaItem(
            id = "morning_evening_protection",
            title = "Protection against Harm (Morning & Evening)",
            urduTitle = "ہر شر و نقصان سے حفاظت کی دعا",
            category = "protection",
            arabic = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
            transliteration = "Bismillahilladhi la yadurru ma'as-mihi shay'un fil-ardi wa la fis-sama'i wa huwas-Sami'ul-'Aleem.",
            urduTranslation = "اللہ کے نام سے جس کے نام کی برکت سے زمین اور آسمان میں کوئی چیز نقصان نہیں پہنچا سکتی اور وہ خوب سننے والا جاننے والا ہے۔",
            translation = "In the name of Allah, with whose name nothing can cause harm on earth or in the heavens, and He is the All-Hearing, the All-Knowing.",
            reference = "Abu Dawud 5088, Tirmidhi 3388"
        )
    )

    val POPULAR_CITIES: List<LocationConfig> = listOf(
        LocationConfig("Karachi", "Pakistan", 24.8607, 67.0011),
        LocationConfig("Lahore", "Pakistan", 31.5204, 74.3587),
        LocationConfig("Islamabad", "Pakistan", 33.6844, 73.0479),
        LocationConfig("Peshawar", "Pakistan", 34.0151, 71.5249),
        LocationConfig("Quetta", "Pakistan", 30.1798, 66.9750),
        LocationConfig("Makkah", "Saudi Arabia", 21.3891, 39.8579),
        LocationConfig("Madinah", "Saudi Arabia", 24.5247, 39.5692),
        LocationConfig("Riyadh", "Saudi Arabia", 24.7136, 46.6753),
        LocationConfig("Dubai", "UAE", 25.2048, 55.2708),
        LocationConfig("Abu Dhabi", "UAE", 24.4539, 54.3773),
        LocationConfig("Doha", "Qatar", 25.2854, 51.5310),
        LocationConfig("Kuwait City", "Kuwait", 29.3759, 47.9774),
        LocationConfig("Istanbul", "Turkey", 41.0082, 28.9784),
        LocationConfig("Cairo", "Egypt", 30.0444, 31.2357),
        LocationConfig("London", "United Kingdom", 51.5074, -0.1278),
        LocationConfig("New York", "United States", 40.7128, -74.0060),
        LocationConfig("Toronto", "Canada", 43.6532, -79.3832),
        LocationConfig("Jakarta", "Indonesia", -6.2088, 106.8456),
        LocationConfig("Kuala Lumpur", "Malaysia", 3.1390, 101.6869),
        LocationConfig("Dhaka", "Bangladesh", 23.8103, 90.4125),
        LocationConfig("Mumbai", "India", 19.0760, 72.8777),
        LocationConfig("Delhi", "India", 28.6139, 77.2090),
        LocationConfig("Sydney", "Australia", -33.8688, 151.2093)
    )
}
