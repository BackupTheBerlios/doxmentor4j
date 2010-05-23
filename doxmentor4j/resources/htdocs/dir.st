<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd"><head>
    <link rel="stylesheet" type="text/css" media="all" href="styles/css-tables.css!+!main.css" > </link>
   <title>$title$</title>
   <META Http-Equiv="Cache-Control" Content="no-cache">
   <META Http-Equiv="Pragma" Content="no-cache">
   <META Http-Equiv="Expires" Content="0">
</head>
<body>
   <table width="100%">
         <caption>$caption$</caption>
         <tbody>
            $if(injar)$
               $dirEntries:resources/htdocs/templates/trtd(),resources/htdocs/templates/td(),resources/htdocs/templates/tdtr()$
            $else$
               $dirEntries:templates/trtd(),templates/td(),templates/tdtr()$
            $endif$
         </tbody>
      </table>
</body>
</html>