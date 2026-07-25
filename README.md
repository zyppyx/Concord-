<div align="center">

<img src="Assets/Concord_logo.svg" alt="Logo do Concord" width="180" />

# Concord

Uma plataforma completa de comunicação composta por aplicações desktop, mobile e backend.

<p align="center">
    <img src="https://img.shields.io/badge/.NET_10-512BD4?style=for-the-badge&logo=.net&logoColor=white"/>
    <img src="https://img.shields.io/badge/ASP.NET_Core-512BD4?style=for-the-badge"/>
    <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white"/>
    <img src="https://img.shields.io/badge/Entity_Framework_Core-512BD4?style=for-the-badge"/>
</p>

<p align="center">
    <img src="https://img.shields.io/badge/Windows-0078D6?style=for-the-badge&logo=windows&logoColor=white"/>
    <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
    <img src="https://img.shields.io/badge/WPF-5C2D91?style=for-the-badge"/>
    <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge"/>
</p>

<p align="center">
    <img src="https://img.shields.io/badge/Real_Time_Chat-00C853?style=for-the-badge"/>
    <img src="https://img.shields.io/badge/WebSockets-FF9800?style=for-the-badge"/>
    <img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white"/>
    <img src="https://img.shields.io/badge/Image_Sharing-E91E63?style=for-the-badge"/>
</p>
----------------------------------------------------

## 📖 Sobre o Projeto

O Concord é uma plataforma de comunicação desenvolvida para proporcionar uma experiência moderna e intuitiva em múltiplas plataformas. O projeto é composto por uma aplicação desktop desenvolvida em WPF, uma aplicação mobile desenvolvida em Kotlin com Jetpack Compose e uma API própria desenvolvida em ASP.NET Core.

Entre seus principais recursos estão a autenticação segura com JWT, comunicação em tempo real utilizando WebSockets, sistema de amizades, notificações, compartilhamento de imagens e gerenciamento completo de mensagens.


## 🖥️ Aplicação Desktop

<p align="center">
    <img src="Assets/loginscreen.png" width="280"/>
    <img src="Assets/contacts.png" width="280"/>
    <img src="Assets/chatscreen.png" width="280"/>
</p>

<p align="center">
    <b>Login</b> • <b>Tela Inicial</b> • <b>Chat em Tempo Real</b>
</p>

---

## 📱 Aplicação Mobile

<p align="center">
    <img src="Assets/androidloginscr.jpg" width="280"/>
    <img src="Assets/androidhomescr.jpg" width="280"/>
    <img src="Assets/androidchatscr.jpg" width="280"/>
</p>

<p align="center">
    <b>Login</b> • <b>Tela Inicial</b> • <b>Chat em Tempo Real</b>
</p>
---

## 🏗️ Arquitetura do Projeto

| Componente | Tecnologias | Responsabilidade |
|------------|------------|------------|
| Concord.Desktop | WPF + .NET 10 | Interface desktop da aplicação. |
| Concord.API | ASP.NET Core + PostgreSQL + EF Core | Gerenciamento de usuários, mensagens, autenticação e comunicação em tempo real. |
| Concord.Android | Kotlin + Jetpack Compose | Interface mobile da aplicação. |

---

## ✨ Principais Funcionalidades

- Chat em tempo real utilizando WebSockets.
- Sistema de autenticação e autorização com JWT.
- Criptografia de senhas utilizando BCrypt.
- Sistema de amizades e gerenciamento de contatos.
- Compartilhamento de imagens no chat.
- Aplicação Desktop desenvolvida em WPF.
- Aplicação Mobile desenvolvida em Kotlin com Jetpack Compose.
- API REST desenvolvida em ASP.NET Core.
- Banco de dados PostgreSQL utilizando Entity Framework Core.
- Comunicação segura entre cliente e servidor.

## 🛠️ Tecnologias utilizadas


| Tecnologia | Utilização |
|------------|------------|
| .NET 10 | Aplicação Desktop |
| WPF | Interface Desktop |
| ASP.NET Core | Backend da aplicação |
| PostgreSQL | Banco de dados |
| Entity Framework Core | ORM |
| JWT | Autenticação |
| BCrypt | Criptografia das senhas |
| WebSockets | Comunicação em tempo real |
| Kotlin | Aplicação Android |
| Jetpack Compose | Interface Mobile |


---

## 📂 Estrutura do Repositório

| Pasta | Descrição |
|------|------|
| Concord.Desktop | Aplicação desktop desenvolvida em WPF (.NET 10) |
| Concord.API | API REST desenvolvida em ASP.NET Core |
| Concord.Android | Aplicação mobile desenvolvida em Kotlin + Jetpack Compose |
| assets | Logos e capturas de tela utilizadas no README |

---

## 🚀 Como Executar o Projeto

### Desktop

1. Abra a solução localizada em `Concord.Desktop`.
2. Configure a URL da API, caso necessário.
3. Execute o projeto pelo Visual Studio.

---

### API

1. Configure a string de conexão do PostgreSQL no `appsettings.json`.
2. Execute as migrations do Entity Framework Core.
3. Inicie a API utilizando o Visual Studio ou o .NET CLI.

---

### Android

1. Abra a pasta `Concord.Android` no Android Studio.
2. Configure a URL da API.
3. Execute o projeto em um dispositivo físico ou emulador Android.


---

## 👨‍💻 Desenvolvedor

Desenvolvido por Danilo Tavares.

- Desktop: WPF + .NET 10
- Backend: ASP.NET Core + PostgreSQL
- Mobile: Kotlin + Jetpack Compose

</div>