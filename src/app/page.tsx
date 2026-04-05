import { redirect } from "next/navigation";

export default function Home() {
  // Eventually we'll check session here and redirect to /login or /dashboard
  redirect("/dashboard");
}
