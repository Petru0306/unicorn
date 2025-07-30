# 🧪 Ghid de Testare UWS-Monitoring

## 📋 Prezentare Generală
Acest ghid conține toate testele necesare pentru a verifica că sistemul UWS-Monitoring funcționează corect, inclusiv autentificarea, colectarea de metrici, generarea de grafice, sistemul de alerte și toate funcționalitățile bonus.

---

## 🚀 1. Pregătirea Mediului de Testare

### 1.1 Pornirea Aplicației
```bash
# Navighează în directorul proiectului
cd unicorn

# Pornește aplicația Spring Boot
./mvnw spring-boot:run
```

### 1.2 Verificarea Pornirii
- ✅ Aplicația pornește fără erori
- ✅ Accesează `http://localhost:8080` - ar trebui să vezi pagina principală
- ✅ Verifică consola pentru mesajele de pornire

---

## 🔐 2. Testarea Autentificării

### 2.1 Înregistrare Utilizator Nou
1. Accesează `http://localhost:8080/register.html`
2. Completează formularul:
   - **Email**: `test@example.com`
   - **Password**: `test123`
3. Apasă "Register"
4. ✅ Verifică că primești mesajul "User registered successfully"

### 2.2 Autentificare
1. Accesează `http://localhost:8080/login.html`
2. Completează:
   - **Email**: `test@example.com`
   - **Password**: `test123`
3. Apasă "Login"
4. ✅ Verifică că primești un JWT token și ești redirecționat către dashboard

### 2.3 Verificarea Token-ului
1. Deschide Developer Tools (F12)
2. Mergi la tab-ul "Application" → "Local Storage"
3. ✅ Verifică că există cheia `jwtToken` cu o valoare

---

## 📊 3. Testarea Dashboard-ului Principal

### 3.1 Accesarea Dashboard-ului
1. După autentificare, ar trebui să fii pe `http://localhost:8080/dashboard.html`
2. ✅ Verifică că vezi toate serviciile UWS
3. ✅ Verifică că există secțiunea "UWS-Monitoring Dashboard" la început

### 3.2 Navigarea către Monitoring
1. Apasă butonul "Access UWS-Monitoring" din secțiunea monitoring
2. ✅ Verifică că ești redirecționat către `/uws-monitoring.html`
3. ✅ Verifică că pagina se încarcă fără erori 403

---

## 📈 4. Testarea Paginii de Monitoring

### 4.1 Verificarea Structurii Paginii
✅ Verifică că vezi:
- **Navigation bar** cu butoane pentru Compact View, Generate Sample Data, Export CSV
- **Time Filter** cu opțiuni: 10 Min, 1 Hour, 24 Hours, 7 Days
- **System Health Overview** cu 4 carduri de statistici
- **Request Volume Chart** (grafic mare)
- **Response Time Statistics** (grafic circular)
- **Service Status** cu 9 servicii
- **CPU Usage Chart** și **RAM Usage Chart**
- **Active Alerts** și **Recent Events**

### 4.2 Verificarea Autentificării pe Pagina de Monitoring
1. Șterge token-ul din Local Storage
2. Încearcă să accesezi `/uws-monitoring.html`
3. ✅ Verifică că ești redirecționat către login
4. Reautentifică-te și verifică că funcționează din nou

---

## 🎯 5. Testarea Generării de Date de Test

### 5.1 Generarea Datelor Inițiale
1. Pe pagina de monitoring, apasă "Generate Sample Data"
2. ✅ Verifică că primești mesajul "Sample data generated successfully!"
3. ✅ Verifică că datele se actualizează automat

### 5.2 Verificarea Datelor Generate
✅ Verifică că:
- **Total Requests** > 0
- **Avg Response Time** > 0ms
- **Error Rate** este un procent valid
- **Active Alerts** = 0 (inițial)

### 5.3 Verificarea Graficelor
✅ Verifică că:
- **Request Volume Chart** arată bare cu numere > 0
- **Response Time Chart** arată un grafic circular cu servicii
- **CPU Usage Chart** arată linii cu valori între 20-80%
- **RAM Usage Chart** arată linii cu valori între 100-500MB

---

## ⏰ 6. Testarea Filtrelor de Timp

### 6.1 Testarea Fiecărui Filtru
1. Apasă "10 Min" - ✅ Verifică că datele se actualizează
2. Apasă "1 Hour" - ✅ Verifică că datele se actualizează
3. Apasă "24 Hours" - ✅ Verifică că datele se actualizează
4. Apasă "7 Days" - ✅ Verifică că datele se actualizează

### 6.2 Verificarea Actualizării în Timp Real
1. Așteaptă 30 de secunde
2. ✅ Verifică că datele se actualizează automat
3. ✅ Verifică că graficele se redesenază

---

## 🚨 7. Testarea Sistemului de Alerte

### 7.1 Crearea unui Alert
1. Apasă "Create Alert"
2. Completează formularul:
   - **Alert Name**: `Test Alert`
   - **Service**: `UWS-S3`
   - **Metric Type**: `Response Time (ms)`
   - **Operator**: `>`
   - **Threshold Value**: `100`
   - **Email Notification**: ✅ Bifează
   - **Webhook URL**: `https://test.com/webhook`
3. Apasă "Create Alert"
4. ✅ Verifică că primești "Alert created successfully!"

### 7.2 Verificarea Alert-ului Creat
1. ✅ Verifică că alert-ul apare în secțiunea "Active Alerts"
2. ✅ Verifică că status-ul este "OK" (inițial)
3. ✅ Verifică că numărul de "Active Alerts" s-a incrementat

### 7.3 Testarea Trigger-ului de Alert
1. Generează din nou date de test
2. ✅ Verifică că alert-ul se poate declanșa (status "CRITICAL")
3. ✅ Verifică că se afișează valoarea curentă în alert

---

## 📊 8. Testarea Exportului de Date

### 8.1 Export CSV
1. Apasă "Export CSV"
2. ✅ Verifică că se descarcă un fișier CSV
3. ✅ Verifică că numele fișierului conține timpul selectat
4. Deschide fișierul și verifică că conține date valide

### 8.2 Testarea Exportului cu Diferite Filtre de Timp
1. Schimbă timpul la "1 Hour"
2. Exportă din nou
3. ✅ Verifică că fișierul conține date diferite

---

## 🎨 9. Testarea Interfeței

### 9.1 Compact View
1. Apasă "Compact View"
2. ✅ Verifică că interfața devine mai compactă
3. ✅ Verifică că butonul devine "Detailed View"
4. Apasă din nou pentru a reveni la view-ul normal

### 9.2 Responsive Design
1. Redimensionează fereastra browser-ului
2. ✅ Verifică că layout-ul se adaptează
3. ✅ Verifică că graficele rămân responsive

---

## 🔄 10. Testarea Actualizării în Timp Real

### 10.1 Verificarea Actualizării Automate
1. Lasă pagina deschisă
2. ✅ Verifică că datele se actualizează la fiecare 30 de secunde
3. ✅ Verifică că graficele se redesenază

### 10.2 Testarea Performanței
1. Deschide Developer Tools → Network
2. ✅ Verifică că request-urile către `/api/monitoring/metrics` sunt rapide
3. ✅ Verifică că nu există erori în consolă

---

## 🧪 11. Testarea Integrării cu Alte Servicii

### 11.1 Testarea Colectării de Metrici
1. Accesează alte servicii UWS (S3, Lambda, etc.)
2. Fă câteva operații (upload fișiere, creează lambda, etc.)
3. Revino la monitoring
4. ✅ Verifică că metricile pentru acele servicii au crescut

### 11.2 Verificarea Aspect-ului de Monitoring
1. Deschide Developer Tools → Console
2. Fă request-uri către API-uri
3. ✅ Verifică că nu există erori legate de monitoring

---

## 🐛 12. Testarea Scenariilor de Eroare

### 12.1 Testarea Autentificării Expirate
1. Modifică token-ul din Local Storage la o valoare invalidă
2. Încearcă să accesezi monitoring-ul
3. ✅ Verifică că ești redirecționat către login

### 12.2 Testarea Request-urilor Fără Token
1. Șterge token-ul din Local Storage
2. Încearcă să faci un request API
3. ✅ Verifică că primești eroare 401/403

### 12.3 Testarea Formularelor Incomplete
1. Încearcă să creezi un alert fără a completa toate câmpurile
2. ✅ Verifică că primești mesaje de eroare corespunzătoare

---

## 📱 13. Testarea Bonus Features

### 13.1 Verificarea Multi-User Support
1. Creează un alt utilizator
2. Autentifică-te cu noul utilizator
3. ✅ Verifică că datele sunt izolate per utilizator

### 13.2 Verificarea Calculului de Uptime
1. ✅ Verifică că se afișează un uptime simulat în dashboard

### 13.3 Verificarea Notificărilor
1. Creează un alert care se declanșează
2. ✅ Verifică că se afișează notificări în UI

---

## ✅ 14. Checklist Final

### 14.1 Funcționalități Core
- [ ] Autentificarea funcționează
- [ ] Pagina de monitoring se încarcă
- [ ] Datele de test se generează
- [ ] Graficele se afișează corect
- [ ] Filtrele de timp funcționează
- [ ] Sistemul de alerte funcționează
- [ ] Exportul CSV funcționează
- [ ] Actualizarea în timp real funcționează

### 14.2 Interfața
- [ ] Design-ul este modern și responsive
- [ ] Compact view funcționează
- [ ] Toate butoanele sunt funcționale
- [ ] Mesajele de eroare sunt clare

### 14.3 Securitatea
- [ ] Autentificarea este obligatorie
- [ ] Token-urile expirate sunt gestionate
- [ ] Datele sunt izolate per utilizator

### 14.4 Performanța
- [ ] Pagina se încarcă rapid
- [ ] Graficele se redesenază eficient
- [ ] Nu există memory leaks

---

## 🎉 15. Concluzie

Dacă toate testele de mai sus trec cu succes, sistemul UWS-Monitoring este gata pentru producție! 

### Raport de Testare
- **Data testării**: _______________
- **Tester**: _______________
- **Versiune aplicație**: _______________
- **Rezultat general**: ✅ PASS / ❌ FAIL
- **Observații**: _______________

---

## 🆘 Troubleshooting

### Probleme Comune

**Eroare 403 la accesarea monitoring-ului:**
- Verifică că ești autentificat
- Verifică că token-ul JWT este valid
- Verifică configurația de securitate din `UnicornApplication.java`

**Graficele nu se afișează:**
- Verifică că Chart.js este încărcat
- Verifică că datele sunt în formatul corect
- Verifică consola pentru erori JavaScript

**Datele nu se actualizează:**
- Verifică că API-ul `/api/monitoring/metrics` răspunde
- Verifică că aspect-ul de monitoring este activ
- Verifică log-urile aplicației

**Alertele nu se declanșează:**
- Verifică că threshold-urile sunt setate corect
- Verifică că datele curente depășesc threshold-urile
- Verifică log-urile pentru erori de procesare 