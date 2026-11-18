package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.CoralCrimsonDim
import com.example.ui.theme.MintCyan
import com.example.ui.theme.MintCyanDim
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceStroke
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

private data class GuideTip(
    val titleEn: String,
    val titleBn: String,
    val descEn: String,
    val descBn: String,
)

@Composable
fun GuideScreen(onClose: () -> Unit) {
    BackHandler { onClose() }

    val bestTips = listOf(
        GuideTip(
            "Front-Facing Portrait",
            "সরাসরি সামনের দিকের ছবি",
            "Choose faces looking straight at the camera with level eyes and a neutral closed mouth.",
            "ক্যামেরার দিকে সোজা তাকানো, চোখ সমান ও মুখ বন্ধ থাকা ছবি ব্যবহার করুন।",
        ),
        GuideTip(
            "Clear, Even Lighting",
            "পরিষ্কার ও সমান আলো",
            "Soft front lighting preserves skin contours so the 478-point mesh blends seamlessly.",
            "নরম সামনের আলো থাকলে ৪৭৮-পয়েন্ট মেশ নিখুঁতভাবে ত্বকের সাথে মিশে যায়।",
        ),
        GuideTip(
            "No Sunglasses or Masks",
            "সানগ্লাস বা মাস্ক ছাড়া",
            "Unobstructed eyes and eyebrows let live blinking and expressions track accurately.",
            "চোখ ও ভ্রু খোলা থাকলে রিয়েল-টাইম চোখের পলক ও অভিব্যক্তি নিখুঁতভাবে কাজ করে।",
        ),
        GuideTip(
            "Realistic Proportions",
            "বাস্তবসম্মত অনুপাত",
            "Real photos and realistic 3D avatars match human jaw, nose, and lip geometry best.",
            "বাস্তব ছবি এবং রিয়ালিস্টিক ৩ডি অ্যাভাটারে চোয়াল ও ঠোঁটের মাপ সবচেয়ে ভালো মেলে।",
        ),
    )

    val avoidTips = listOf(
        GuideTip(
            "Side Profiles & Extreme Tilts",
            "একপাশ বা অতিরিক্ত বাঁকা মুখ",
            "Turned heads hide landmarks on one cheek and cause stretched mesh edges.",
            "মাথা ঘুরে থাকলে এক পাশের ল্যান্ডমার্ক ঢাকা পড়ে এবং ছবি টেনে যায়।",
        ),
        GuideTip(
            "Sunglasses, Hair, or Hands",
            "সানগ্লাস, চুল বা হাত দিয়ে ঢাকা",
            "Objects covering eyes or lips block MediaPipe landmark detection.",
            "চোখ বা ঠোঁটের উপর কিছু থাকলে মিডিয়াপাইপ মুখ সঠিকভাবে শনাক্ত করতে পারে না।",
        ),
        GuideTip(
            "Heavy Shadows or Cartoon Distortion",
            "গাঢ় ছায়া বা অতিরঞ্জিত কার্টুন",
            "Harsh backlighting or exaggerated anime eyes warp awkwardly on real faces.",
            "তীব্র ছায়া বা অতিরিক্ত বড় কার্টুন চোখ আসল মুখের সাথে অস্বাভাবিকভাবে বসে।",
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("guide_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back • ফিরে যান",
                    tint = TextPrimary,
                )
            }
            Column {
                Text(
                    text = "Face Guide • ফেস গাইড",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                )
                Text(
                    text = "Which portraits swap cleanly • কোন ছবিগুলো সবচেয়ে ভালো কাজ করে",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SurfaceStroke),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "How FaceSwap Cam Works • যেভাবে কাজ করে",
                        style = MaterialTheme.typography.titleMedium,
                        color = MintCyan,
                    )
                    Text(
                        text = "MediaPipe tracks 478 facial points + blendshapes on GPU and warps your chosen face in real time using One Euro smoothing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )
                    Text(
                        text = "মিডিয়াপাইপ জিপিইউ-তে ৪৭৮টি পয়েন্ট ট্র্যাক করে এবং ওয়ান-ইউরো ফিল্টার দিয়ে রিয়েল-টাইমে মুখ বসায়।",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MintCyan)
                Text(
                    text = "Works Best • সেরা ফলাফলের জন্য",
                    style = MaterialTheme.typography.titleMedium,
                    color = MintCyan,
                )
            }

            bestTips.forEach { tip ->
                GuideTipCard(tip = tip, isPositive = true)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Icon(Icons.Filled.Cancel, contentDescription = null, tint = CoralCrimson)
                Text(
                    text = "Avoid These • যা এড়িয়ে চলবেন",
                    style = MaterialTheme.typography.titleMedium,
                    color = CoralCrimson,
                )
            }

            avoidTips.forEach { tip ->
                GuideTipCard(tip = tip, isPositive = false)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GuideTipCard(tip: GuideTip, isPositive: Boolean) {
    val borderColor = if (isPositive) MintCyan.copy(alpha = 0.45f) else CoralCrimson.copy(alpha = 0.45f)
    val bgColor = if (isPositive) MintCyanDim else CoralCrimsonDim

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${tip.titleEn} • ${tip.titleBn}",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = tip.descEn,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary.copy(alpha = 0.9f),
            )
            Text(
                text = tip.descBn,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
    }
}
