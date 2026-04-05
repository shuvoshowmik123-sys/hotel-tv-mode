export function SkeletonCard({ rows = 3 }: { rows?: number }) {
    return (
        <div className="bento-card">
            {/* Header skeleton */}
            <div className="bento-card-header">
                <div>
                    <div className="skeleton h-3 w-20 mb-2" />
                    <div className="skeleton h-5 w-36" />
                </div>
                <div className="skeleton h-9 w-24 rounded-full" />
            </div>
            {/* Row skeletons */}
            <div className="space-y-4 mt-4">
                {Array.from({ length: rows }).map((_, i) => (
                    <div key={i} className="flex gap-4 items-center" style={{ opacity: 1 - i * 0.15 }}>
                        <div className="skeleton-gold h-12 w-14 rounded-xl shrink-0" />
                        <div className="flex-1 space-y-2">
                            <div className="skeleton h-4 rounded" style={{ width: `${70 + (i % 3) * 10}%` }} />
                            <div className="skeleton h-3 rounded w-1/2" />
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}

export function SkeletonStatCard() {
    return (
        <div className="bento-card">
            <div className="skeleton h-3 w-24 mb-4" />
            <div className="skeleton-gold h-10 w-16 mb-2 rounded-lg" />
            <div className="skeleton h-3 w-32" />
        </div>
    );
}
