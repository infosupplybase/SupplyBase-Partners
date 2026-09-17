import { Navigate, Route, Routes } from 'react-router-dom'
import AppShell from './components/AppShell'
import { RequireAuth } from './components/RouteGuards'
import { AuthProvider } from './context/AuthContext'
import { ToastProvider } from './context/ToastContext'
import AroundYouTab from './pages/dashboard/AroundYouTab'
import CommunityTab from './pages/dashboard/CommunityTab'
import JobDetailPage from './pages/dashboard/JobDetailPage'
import MoneyTab from './pages/dashboard/MoneyTab'
import NotificationsPage from './pages/dashboard/NotificationsPage'
import ProfilePage from './pages/dashboard/ProfilePage'
import ProgressTab from './pages/dashboard/ProgressTab'
import ScreeningPage from './pages/dashboard/ScreeningPage'
import StarterKitPage from './pages/dashboard/StarterKitPage'
import SupportPage from './pages/dashboard/SupportPage'
import TrainingPage from './pages/dashboard/TrainingPage'
import VerificationPage from './pages/dashboard/VerificationPage'
import BasicDetails from './pages/onboarding/BasicDetails'
import City from './pages/onboarding/City'
import EarningPotential from './pages/onboarding/EarningPotential'
import Permissions from './pages/onboarding/Permissions'
import TermsPrivacy from './pages/onboarding/TermsPrivacy'
import WorkCategory from './pages/onboarding/WorkCategory'
import WorkingHours from './pages/onboarding/WorkingHours'
import OtpAuth from './pages/OtpAuth'
import Welcome from './pages/Welcome'

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <Routes>
          <Route path="/" element={<Welcome />} />
          <Route path="/join" element={<OtpAuth />} />
          <Route path="/login" element={<OtpAuth />} />

          <Route element={<RequireAuth />}>
            <Route path="/onboarding/basic-details" element={<BasicDetails />} />
            <Route path="/onboarding/category" element={<WorkCategory />} />
            <Route path="/onboarding/city" element={<City />} />
            <Route path="/onboarding/terms" element={<TermsPrivacy />} />
            <Route path="/onboarding/earning-potential" element={<EarningPotential />} />
            <Route path="/onboarding/working-hours" element={<WorkingHours />} />
            <Route path="/onboarding/permissions" element={<Permissions />} />

            <Route path="/app" element={<AppShell />}>
              <Route index element={<Navigate to="progress" replace />} />
              <Route path="progress" element={<ProgressTab />} />
              <Route path="progress/verification" element={<VerificationPage />} />
              <Route path="progress/screening" element={<ScreeningPage />} />
              <Route path="progress/starter-kit" element={<StarterKitPage />} />
              <Route path="progress/profile" element={<ProfilePage />} />
              <Route path="progress/training" element={<TrainingPage />} />
              <Route path="money" element={<MoneyTab />} />
              <Route path="around-you" element={<AroundYouTab />} />
              <Route path="community" element={<CommunityTab />} />
              <Route path="profile" element={<ProfilePage />} />
              <Route path="support" element={<SupportPage />} />
              <Route path="notifications" element={<NotificationsPage />} />
              <Route path="jobs/:jobId" element={<JobDetailPage />} />
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </ToastProvider>
    </AuthProvider>
  )
}
