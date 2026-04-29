import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login';
import { DashboardComponent } from './components/dashboard/dashboard';
import { WalletComponent } from './components/wallet/wallet';
import { ProfileComponent } from './components/profile/profile';
import { ContactsComponent } from './components/contacts/contacts';
import { RegisterComponent } from './components/register/register';
import { MfaSetupComponent } from './components/mfa-setup/mfa-setup';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'mfa-setup', component: MfaSetupComponent },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'wallet', component: WalletComponent },
  { path: 'profile', component: ProfileComponent },
  { path: 'contacts', component: ContactsComponent },
  { path: '**', redirectTo: '/login' }
];