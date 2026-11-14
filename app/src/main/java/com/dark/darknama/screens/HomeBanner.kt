package com.dark.darknama.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.dark.darknama.R
import com.dark.darknama.components.settingsFocusBorder
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.data.model.HomeSlide
import com.dark.darknama.ui.home.HomeBannerViewModel
import com.dark.darknama.ui.theme.glassBorder
import com.dark.darknama.utils.DeviceUtils
import com.dark.darknama.utils.StorageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun HomeBanner(navController: NavController?, viewModel: HomeBannerViewModel = viewModel()) {
    val context = LocalContext.current
    val isTv = remember(context) { DeviceUtils.isTv(context) }
    if (isTv) {
        TvHomeBanner(viewModel)
        return
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val bannerPadding = if (isTv) maxOf(maxWidth * 0.14f, (maxWidth - 850.dp) / 2) else 16.dp
        val bannerHeight = if (isTv) (maxWidth - bannerPadding * 2) * (7f / 16f) else 296.dp
        val slides = viewModel.slides
        when {
            viewModel.isLoading && slides.isEmpty() -> BannerShimmer(bannerHeight, bannerPadding)
            viewModel.hasError && slides.isEmpty() -> {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.home_banner_error),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f))
                    Button(onClick = viewModel::load,
                        modifier = Modifier.tvFocusItem("banner-retry").tvFocusBorder(CircleShape)) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
            slides.isEmpty() -> Text(stringResource(R.string.home_banner_empty),
                modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> {
                val pagerState = rememberPagerState(pageCount = { slides.size })
                val scope = rememberCoroutineScope()
                var touching by remember { mutableStateOf(false) }
                var prevFocused by remember { mutableStateOf(false) }
                var nextFocused by remember { mutableStateOf(false) }
                // Several pager pages may be composed at once. Track the focused slide,
                // not a shared Boolean that an offscreen page can clear.
                var focusedSlideKey by remember { mutableStateOf<String?>(null) }
                val paused = touching || prevFocused || nextFocused || focusedSlideKey != null
                LaunchedEffect(pagerState, slides.size, paused) {
                    if (slides.size > 1 && !paused) {
                        while (true) {
                            delay(5_000)
                            if (!pagerState.isScrollInProgress) {
                                pagerState.animateScrollToPage((pagerState.currentPage + 1) % slides.size)
                            }
                        }
                    }
                }
                Column(modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        touching = true
                        try { waitForUpOrCancellation() } finally { touching = false }
                    }
                }) {
                    HorizontalPager(state = pagerState, contentPadding = PaddingValues(horizontal = bannerPadding),
                        pageSpacing = 8.dp, modifier = Modifier.fillMaxWidth().height(bannerHeight)) { page ->
                        val slide = slides[page]
                        val shape = RoundedCornerShape(18.dp)
                        Box(modifier = Modifier.fillMaxSize().clip(shape).glassBorder(shape)) {
                            AsyncImage(model = slide.backdrop, contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
                            Box(Modifier.fillMaxSize().background(
                                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.14f),
                                    Color.Black.copy(alpha = 0.86f)))))
                            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp)) {
                                Text(slide.title, style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis, color = Color.White)
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(if (slide is HomeSlide.MovieSlide) R.string.movies else R.string.series),
                                        style = MaterialTheme.typography.labelMedium, color = Color.White)
                                    if (slide.imdb > 0) {
                                        Icon(Icons.Default.Star, contentDescription = null,
                                            tint = Color(0xFFFFCA28), modifier = Modifier.size(16.dp))
                                        Text(String.format(Locale.getDefault(), "%.1f", slide.imdb),
                                            style = MaterialTheme.typography.labelMedium, color = Color.White)
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Button(onClick = {
                                    when (slide) {
                                        is HomeSlide.MovieSlide -> {
                                            StorageUtils.saveMovieToFile(context, slide.movie)
                                            navController?.navigate("single_movie/${slide.movie.id}")
                                        }
                                        is HomeSlide.SeriesSlide -> {
                                            StorageUtils.saveSeriesToFile(context, slide.series)
                                            navController?.navigate("single_series/${slide.series.id}")
                                        }
                                    }
                                }, modifier = Modifier.tvFocusItem("banner-details-${slide.key}")
                                    .onFocusChanged {
                                        if (it.isFocused) focusedSlideKey = slide.key
                                        else if (focusedSlideKey == slide.key) focusedSlideKey = null
                                    }
                                    .settingsFocusBorder(focusedSlideKey == slide.key, CircleShape)) {
                                    Text(stringResource(R.string.home_banner_details))
                                }
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        if (isTv && slides.size > 1) {
                            IconButton(onClick = { scope.launch {
                                pagerState.animateScrollToPage((pagerState.currentPage - 1 + slides.size) % slides.size)
                            } }, modifier = Modifier.tvFocusItem("banner-previous")
                                .onFocusChanged { prevFocused = it.isFocused }
                                .settingsFocusBorder(prevFocused, CircleShape)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.home_banner_previous),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        slides.forEachIndexed { index, _ ->
                            Box(Modifier.padding(horizontal = 3.dp).size(if (index == pagerState.currentPage) 9.dp else 6.dp)
                                .clip(CircleShape).background(if (index == pagerState.currentPage)
                                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                        if (isTv && slides.size > 1) {
                            IconButton(onClick = { scope.launch {
                                pagerState.animateScrollToPage((pagerState.currentPage + 1) % slides.size)
                            } }, modifier = Modifier.tvFocusItem("banner-next")
                                .onFocusChanged { nextFocused = it.isFocused }
                                .settingsFocusBorder(nextFocused, CircleShape)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = stringResource(R.string.home_banner_next),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Decorative TV carousel: it never enters the D-pad focus tree. */
@Composable
private fun TvHomeBanner(viewModel: HomeBannerViewModel) {
    val slides = viewModel.slides
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val padding = maxOf(maxWidth * 0.14f, (maxWidth - 850.dp) / 2)
        val height = (maxWidth - padding * 2) * (7f / 16f)
        val edgeFadeWidth = maxWidth * 0.12f
        when {
            viewModel.isLoading && slides.isEmpty() -> BannerShimmer(height, padding)
            viewModel.hasError && slides.isEmpty() -> Text(stringResource(R.string.home_banner_error),
                modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            slides.isEmpty() -> Text(stringResource(R.string.home_banner_empty),
                modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> {
                val initialPage = remember(slides.size) { (Int.MAX_VALUE / 2 / slides.size) * slides.size }
                val pager = androidx.compose.runtime.key(slides.size) {
                    rememberPagerState(initialPage = initialPage, pageCount = { Int.MAX_VALUE })
                }
                LaunchedEffect(pager, slides.size) {
                    if (slides.size > 1) while (true) {
                        delay(5_000)
                        if (pager.currentPage == Int.MAX_VALUE - 1) pager.scrollToPage(initialPage)
                        else pager.animateScrollToPage(pager.currentPage + 1)
                    }
                }
                Column {
                    Box(Modifier.fillMaxWidth().height(height)) {
                        HorizontalPager(state = pager, userScrollEnabled = false,
                            contentPadding = PaddingValues(horizontal = padding), pageSpacing = 8.dp,
                            modifier = Modifier.fillMaxSize().focusProperties { canFocus = false }) { page ->
                            val slide = slides[page % slides.size]
                            val shape = RoundedCornerShape(18.dp)
                            Box(Modifier.fillMaxSize().clip(shape).glassBorder(shape)) {
                                AsyncImage(model = slide.backdrop, contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
                                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.14f), Color.Black.copy(alpha = 0.86f)))))
                                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp)) {
                                    Text(slide.title, style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold, maxLines = 2,
                                        overflow = TextOverflow.Ellipsis, color = Color.White)
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(stringResource(if (slide is HomeSlide.MovieSlide) R.string.movies else R.string.series),
                                            style = MaterialTheme.typography.labelMedium, color = Color.White)
                                        if (slide.imdb > 0) {
                                            Icon(Icons.Default.Star, contentDescription = null,
                                                tint = Color(0xFFFFCA28), modifier = Modifier.size(16.dp))
                                            Text(String.format(Locale.getDefault(), "%.1f", slide.imdb),
                                                style = MaterialTheme.typography.labelMedium, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                        val background = MaterialTheme.colorScheme.background.copy(alpha = 1f)
                        Box(Modifier.align(Alignment.CenterStart).width(edgeFadeWidth).height(height)
                            .background(Brush.horizontalGradient(listOf(background, Color.Transparent))))
                        Box(Modifier.align(Alignment.CenterEnd).width(edgeFadeWidth).height(height)
                            .background(Brush.horizontalGradient(listOf(Color.Transparent, background))))
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center) {
                        slides.forEachIndexed { index, _ ->
                            val selected = index == pager.currentPage % slides.size
                            Box(Modifier.padding(horizontal = 3.dp).size(if (selected) 9.dp else 6.dp)
                                .clip(CircleShape).background(if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerShimmer(height: Dp, horizontalPadding: Dp) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "bannerShimmer")
    val offset by transition.animateFloat(-350f, 350f,
        infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "gradient")
    Box(Modifier.fillMaxWidth().padding(horizontal = horizontalPadding)
        .height(height).clip(RoundedCornerShape(18.dp))
        .background(Brush.linearGradient(listOf(scheme.surfaceVariant,
            scheme.onSurface.copy(alpha = 0.16f), scheme.surfaceVariant),
            start = Offset(offset, 0f), end = Offset(offset + 340f, 320f))))
}
