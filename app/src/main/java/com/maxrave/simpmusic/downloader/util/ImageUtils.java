package com.maxrave.simpmusic.downloader.util;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static org.schabi.newpipe.extractor.Image.HEIGHT_UNKNOWN;
import static org.schabi.newpipe.extractor.Image.WIDTH_UNKNOWN;

import android.content.Context;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;
import com.maxrave.simpmusic.R;

import org.schabi.newpipe.extractor.Image;

import java.util.Comparator;
import java.util.List;

public class ImageUtils {

    public static String choosePreferredImage(@NonNull final List<Image> images) {
        // this will be used to estimate the pixel count for images where only one of height or
        // width are known
        final double widthOverHeight = images.stream()
                .filter(image -> image.getHeight() != HEIGHT_UNKNOWN && image.getWidth() != WIDTH_UNKNOWN)
                .mapToDouble(image -> ((double) image.getWidth()) / image.getHeight())
                .findFirst()
                .orElse(1.0);

        final Comparator<Image> initialComparator = Comparator
                // the first step splits the images into groups of resolution levels
                .<Image>comparingInt(i -> {
                    if (i.getEstimatedResolutionLevel() == Image.ResolutionLevel.UNKNOWN) {
                        return 3; // avoid unknowns as much as possible
                    } else if (i.getEstimatedResolutionLevel() == Image.ResolutionLevel.MEDIUM) {
                        return 1; // the preferredLevel is only 1 "step" away (either HIGH or LOW)
                    } else {
                        return 2; // the preferredLevel is the furthest away possible (2 "steps")
                    }
                })
                // then each level's group is further split into two subgroups, one with known image
                // size (which is also the preferred subgroup) and the other without
                .thenComparing(image -> image.getHeight() == HEIGHT_UNKNOWN && image.getWidth() == WIDTH_UNKNOWN);

        // The third step chooses, within each subgroup with known image size, the best image based
        // on how close its size is to BEST_LOW_H or BEST_MEDIUM_H (with proper units). Subgroups
        // without known image size will be left untouched since estimatePixelCount always returns
        // the same number for those.
        final Comparator<Image> finalComparator = initialComparator.thenComparingDouble(
                // this is reversed with a - so that the highest resolution is chosen
                i -> -estimatePixelCount(i, widthOverHeight));

        return images.stream()
                // using "min" basically means "take the first group, then take the first subgroup,
                // then choose the best image, while ignoring all other groups and subgroups"
                .min(finalComparator)
                .map(Image::getUrl)
                .orElse(null);
    }

    private static double estimatePixelCount(final Image image, final double widthOverHeight) {
        if (image.getHeight() == HEIGHT_UNKNOWN) {
            if (image.getWidth() == WIDTH_UNKNOWN) {
                // images whose size is completely unknown will be in their own subgroups, so
                // any one of them will do, hence returning the same value for all of them
                return 0;
            } else {
                return image.getWidth() * image.getWidth() / widthOverHeight;
            }
        } else if (image.getWidth() == WIDTH_UNKNOWN) {
            return image.getHeight() * image.getHeight() * widthOverHeight;
        } else {
            return image.getHeight() * image.getWidth();
        }
    }

    public static void loadThumbnail(Context context, ImageView imageView, List<Image> images) {
        if (context != null) {
            Glide.with(context).load(choosePreferredImage(images))
                    .transition(withCrossFade(new DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.holder_video)
                    .fallback(R.drawable.holder_video)
                    .into(imageView);
        }
    }
}
