import React from "react";

export function Icon({ name, className = "" }: { name: string, className?: string }) {
    const paths: Record<string, React.ReactNode> = {
        dashboard: <><path d="M3 12h7V3H3z" /><path d="M14 21h7v-7h-7z" /><path d="M14 10h7V3h-7z" /><path d="M3 21h7v-4H3z" /></>,
        binding: <><path d="M8 12a4 4 0 0 1 0-6l2-2a4 4 0 0 1 6 6l-1 1" /><path d="M16 12a4 4 0 0 1 0 6l-2 2a4 4 0 0 1-6-6l1-1" /></>,
        rooms: <><path d="M4 20V8l8-4 8 4v12" /><path d="M2 20h20" /><path d="M9 20v-6h6v6" /></>,
        sessions: <><path d="M8 7V3" /><path d="M16 7V3" /><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M3 10h18" /></>,
        content: <><path d="M4 18V6a2 2 0 0 1 2-2h12" /><path d="M8 22h10a2 2 0 0 0 2-2V8" /><path d="M10 12h6" /><path d="M10 16h6" /></>,
        menus: <><path d="M8 5h13" /><path d="M8 12h13" /><path d="M8 19h13" /><path d="M3 5h.01" /><path d="M3 12h.01" /><path d="M3 19h.01" /></>,
        policies: <><path d="M4 7h10" /><path d="M14 7a2 2 0 1 0 4 0a2 2 0 1 0-4 0" /><path d="M10 17h10" /><path d="M6 17a2 2 0 1 0 4 0a2 2 0 1 0-4 0" /></>,
        users: <><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M22 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" /></>,
        audit: <><path d="M12 8v5l4 2" /><circle cx="12" cy="12" r="9" /></>,
        settings: <><path d="M12 15a3 3 0 1 0 0-6" /><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33" /><path d="M4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33" /><path d="M3 12a2 2 0 0 1 2-2h.09" /><path d="M21 12a2 2 0 0 1-2 2h-.09" /></>,
        bell: <><path d="M15 17h5l-1.4-1.4A2 2 0 0 1 18 14.2V11a6 6 0 1 0-12 0v3.2a2 2 0 0 1-.6 1.4L4 17h5" /><path d="M10 21a2 2 0 0 0 4 0" /></>,
        link: <><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" /><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" /></>
    };

    return (
        <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className={`w-5 h-5 ${className}`}
            aria-hidden="true"
        >
            {paths[name] || paths.dashboard}
        </svg>
    );
}
