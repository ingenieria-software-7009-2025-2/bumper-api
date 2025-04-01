\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{config-wokSH}[2024/01/26 WokerSH]
\RequirePackage[utf8]{inputenc}

\LoadClass[12pt, a4paper]{article}

\RequirePackage{fancybox}

%-----------------------CONFIGURACIÓN DEL TITULO-------------------/
%-----------------------------------------------------------------/
\renewcommand\maketitle{
    \begin{flushleft}
        \textsc{Ingenieria de Software} \\
        \textbf{Semestre: \semestre} \\
        Equipo: \textsc{Bumper}\\
        Fecha: \fecha \\
        %NoCuenta: \id
    \end{flushleft}
    \begin{flushright}\vspace{-20mm}
        \includegraphics[height=3cm]{IMA/Ciencias.png}
    \end{flushright}

    \vspace{10mm}
    
    \begin{center}\vspace{-1cm}
        \textbf{\Large \nombreTarea}\\
        %\subtitulo
    \end{center}


    \begin{center}
        \rule[1.5ex]{1\linewidth}{0.5mm} 
    \end{center}
    
}
%-----------------------------------------------------------------\
%-----------------------CONFIGURACIÓN DEL TITULO-------------------\


%---------------------------------/
%--------------------------------/
\usepackage[                 %   |
  headheight=15pt,  %            |
  letterpaper,  % Tipo de pag.   |
  left =2cm,  %  < 1 >           |
  right =2cm, %  < 1 >           | MARGENES DE LA PAGINA
  top =2cm,     %  < 1.5 >       |
  bottom =1.5cm %  < 1.5 >       |
]{geometry}     %                |
%--------------------------------\
%---------------------------------\


%-------------------------------------------------------------|
%-------------Lista de estilos códigos------------------------|
\RequirePackage{listings}
%-------------------------------------------------------------|

%//////////////// ESTILO JAVA --------------------------------|
\lstdefinestyle{mystylejava}{
    keywordstyle=[1]\color{codeclassjava},
    backgroundcolor=\color{backcolour},
    commentstyle=\color{codegreen},
    numberstyle=\tiny\color{codenumizq},
    stringstyle=\color{codestringjava},
    basicstyle=\footnotesize\ttfamily\color{white},
    breakatwhitespace=false,
    breaklines=true,
    captionpos=b,
    keepspaces=true,
    numbers=left,
    numbersep=5pt,
    showspaces=false,
    showstringspaces=false,
    showtabs=false,
    tabsize=2,            
    otherkeywords={print},
    escapechar=\&,
    literate=                
        {;}{{\textcolor{yellowpoint}{;}}}1
        {+}{{\textcolor{yellowpoint}{+}}}1
        {-}{{\textcolor{yellowpoint}{-}}}1
        {\{}{{\textcolor{yellowpoint}{\{}}}1
        {\}}{{\textcolor{yellowpoint}{\}}}}1
        {[}{{\textcolor{yellowpoint}{[}}}1
        {]}{{\textcolor{yellowpoint}{]}}}1
        {=}{{\textcolor{yellowpoint}{=}}}1
    %frame=shadowbox
}
%//////////////// ESTILO JAVA --------------------------------|

%  % %  %%  %%  %%  %%  %%  %%  %%  %%  %%  %%  %%  %%  %%  %

%//////////////// ESTILO PYTHON ------------------------------|
\lstdefinestyle{mystylepython}{
    backgroundcolor=\color{backcolourPY},
    commentstyle=\color{codegreenPY},
    keywordstyle=\color{codeclassPY},
    numberstyle=\tiny\color{codenumizq},
    stringstyle=\color{codestringPY},
    basicstyle=\footnotesize\ttfamily\color{white},
    breakatwhitespace=false,
    breaklines=true,
    captionpos=b,
    keepspaces=true,
    numbers=left,
    numbersep=5pt,
    showspaces=false,
    showstringspaces=false,
    showtabs=false,
    tabsize=2,
    escapechar=\&,
    literate=                
        {;}{{\textcolor{yellowpoint}{;}}}1
        {+}{{\textcolor{yellowpoint}{+}}}1
        {-}{{\textcolor{yellowpoint}{-}}}1
        {\{}{{\textcolor{yellowpoint}{\{}}}1
        {\}}{{\textcolor{yellowpoint}{\}}}}1
        {[}{{\textcolor{yellowpoint}{[}}}1
        {]}{{\textcolor{yellowpoint}{]}}}1
        {=}{{\textcolor{yellowpoint}{=}}}1
}
%//////////////// ESTILO PYTHON ------------------------------|

%-------------------------------------------------------------|
%-------------Lista de estilos códigos------------------------|
%-------------------------------------------------------------|
%\lstset{style=mystyle}


%-------------------------------------------------------------|
%------------- Modificación de items -------------------------|
\RequirePackage{enumitem}

\newlist{myitemize}{itemize}{4}
\setlist[myitemize,1]{label=$\bullet$, left=1.6em}
\setlist[myitemize,2]{label=$\diamond$, left=1em}
\setlist[myitemize,3]{label=$\rightarrow$, left=1em}
\setlist[myitemize,4]{label=$\blacksquare$, left=1em}

\endinput

%-------------------------------------------------------------|
%------------- Modificación de items -------------------------|

% Configuración de colores para Python en listings
\definecolor{codeblue}{rgb}{0.25, 0.5, 0.75}
\definecolor{codegreen}{rgb}{0, 0.6, 0}
\definecolor{codegray}{rgb}{0.5, 0.5, 0.5}
\definecolor{codepurple}{rgb}{0.58, 0, 0.82}
\definecolor{backcolour}{rgb}{0.95, 0.95, 0.92}

\lstdefinestyle{mystyle}{
    backgroundcolor=\color{backcolour},   
    commentstyle=\color{codegreen},
    keywordstyle=\color{codepurple},
    numberstyle=\tiny\color{codegray},
    stringstyle=\color{codeblue},
    basicstyle=\small,
    breakatwhitespace=false,         
    breaklines=true,                 
    captionpos=b,                    
    keepspaces=true,                 
    numbers=left,                    
    numbersep=5pt,                  
    showspaces=false,                
    showstringspaces=false,
    showtabs=false,                  
    tabsize=4
}
