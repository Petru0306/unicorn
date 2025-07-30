# UWS-Billing Testing Guide

## Problema rezolvată
Prețurile erau prea mici (ex: $0.0000002 per request) și cu doar câteva request-uri nu se vedea schimbarea în dashboard. Am mărit prețurile și am îmbunătățit afișarea pentru a arăta chiar și valorile foarte mici.

## Modificări făcute

### 1. Prețuri mărite pentru vizibilitate mai bună:
- **UWS-S3**: PUT request: $0.0005 → $0.05, GET request: $0.0004 → $0.04
- **UWS-Lambda**: Request: $0.0000002 → $0.02
- **NoSQL**: RCU/WCU: $0.00025 → $0.025
- **SQS**: Request/Message: $0.0000004 → $0.04
- **AI Service**: Request: $0.0001 → $0.01
- **DNS Service**: Query: $0.0000004 → $0.04

### 2. Afișare îmbunătățită pentru valori mici:
- Valorile < $0.01 se afișează cu 6 zecimale
- Valorile < $1 se afișează cu 4 zecimale
- Valorile ≥ $1 se afișează normal

### 3. BillingAspect integrat:
- Înregistrează automat evenimente de billing pentru toate request-urile
- Extrage user ID din JWT
- Mapează serviciile și operațiile la tipurile de resurse corecte

## Ghid de testare

### Pasul 1: Pornește aplicația
```bash
mvn spring-boot:run
```

### Pasul 2: Autentifică-te
1. Mergi la `http://localhost:8080/login.html`
2. Autentifică-te cu un cont existent

### Pasul 3: Testează billing-ul
1. Mergi la `http://localhost:8080/uws-billing.html`
2. Vei vedea dashboard-ul cu costuri $0.00 inițial

### Pasul 4: Generează date de test
1. Apasă butonul **"Generate Sample Data"**
2. Vei vedea costuri populate cu date simulate

### Pasul 5: Testează billing real
1. Apasă butonul **"Test Billing"**
2. Vei vedea un eveniment de billing înregistrat
3. Costul total ar trebui să crească cu $0.02 (prețul pentru un request de test)

### Pasul 6: Testează serviciile reale
1. Mergi la **UWS-S3** și încarcă un fișier
2. Mergi la **UWS-Lambda** și creează o funcție
3. Mergi la **UWS-Compute** și creează o instanță
4. Revino la **UWS-Billing** și verifică că costurile au crescut

### Pasul 7: Verifică afișarea valorilor mici
- Un singur request S3 PUT: $0.05
- Un singur request Lambda: $0.02
- Un singur request SQS: $0.04
- Aceste valori ar trebui să fie vizibile în dashboard

## Ce să verifici

### ✅ Dashboard-ul se încarcă fără erori
### ✅ Butonul "Test Billing" funcționează
### ✅ Butonul "Generate Sample Data" funcționează
### ✅ Costurile se afișează cu zecimale (ex: $0.050000)
### ✅ După request-uri reale, costurile cresc
### ✅ Graficele se actualizează
### ✅ Tabelele se populează cu date

## Debugging

### Dacă nu vezi costuri:
1. Verifică consola browser-ului pentru erori
2. Verifică log-urile aplicației pentru erori de billing
3. Verifică că ești autentificat (JWT token valid)

### Dacă costurile sunt $0.00:
1. Verifică că BillingAspect este activ
2. Verifică că user ID-ul este extras corect din JWT
3. Verifică că evenimentele sunt salvate în baza de date

### Dacă prețurile par încă prea mici:
1. Poți mări și mai mult prețurile în `BillingService.java`
2. Poți modifica funcția `formatCurrency` pentru mai multe zecimale

## Exemple de costuri așteptate

### După 1 request:
- S3 PUT: $0.05
- Lambda: $0.02
- SQS: $0.04
- AI Service: $0.01

### După 10 request-uri:
- S3 PUT: $0.50
- Lambda: $0.20
- SQS: $0.40
- AI Service: $0.10

### După 100 request-uri:
- S3 PUT: $5.00
- Lambda: $2.00
- SQS: $4.00
- AI Service: $1.00

## Note importante

1. **Prețurile sunt mărite doar pentru testare** - în producție ar trebui să fie mai mici
2. **BillingAspect înregistrează automat** toate request-urile la servicii
3. **Costurile se calculează în timp real** pentru fiecare operație
4. **User isolation** - fiecare utilizator vede doar propriile costuri
5. **Multi-user support** - sistemul suportă mai mulți utilizatori simultan

## Următorii pași

1. Testează cu utilizatori diferiți
2. Testează cu volume mari de request-uri
3. Testează alert-urile de billing
4. Testează export-ul de date
5. Ajustează prețurile pentru producție 