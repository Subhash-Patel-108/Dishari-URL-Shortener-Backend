package com.dishari.in.infrastructure.metadata;

// Internal record — accumulates best values from all strategies
public record RawMetadata(
        String title,
        String description,
        String imageUrl,
        String faviconUrl,
        String siteName,
        String author,
        String canonicalUrl,
        String type         // article, video, website...
) {
    public boolean hasTitle() {
        return title != null && !title.isBlank();
    }

    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isBlank();
    }

    public static RawMetadata empty() {
        return new RawMetadata(
                null, null, null, null, null, null, null, null);
    }

    // Merge — prefer non-null values from other
    public RawMetadata mergeWith(RawMetadata other) {
        return new RawMetadata(
                firstNonBlank(this.title,        other.title),
                firstNonBlank(this.description,  other.description),
                firstNonBlank(this.imageUrl,     other.imageUrl),
                firstNonBlank(this.faviconUrl,   other.faviconUrl),
                firstNonBlank(this.siteName,     other.siteName),
                firstNonBlank(this.author,       other.author),
                firstNonBlank(this.canonicalUrl, other.canonicalUrl),
                firstNonBlank(this.type,         other.type)
        );
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        return b;
    }
}