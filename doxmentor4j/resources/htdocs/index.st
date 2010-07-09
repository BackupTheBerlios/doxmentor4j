<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">
<head>
   <title>DoxMentor4J</title>
   <!-- HttpdLite extension - Allow multiple .css or .js files to be clumped together for improved performance -->
   <link rel="stylesheet" type="text/css" media="all" href="styles/tree.css!+!main.css!+!tabs.css" > </link>
   <script type="text/javascript" src="scripts/prototype.js!+!tree.js!+!tabs.js!+!index.js!+!search.js" > </script>
   <script language="javascript">
         resetTreeDefaults();
         Tree.url = "/node"; 
         appendLoader(initTabs);              
	</script>
   <META Http-Equiv="Cache-Control" Content="no-cache">
   <META Http-Equiv="Pragma" Content="no-cache">
   <META Http-Equiv="Expires" Content="Sat, 26 Jul 1997 05:00:00 GMT">
</head>
<body> 
   <div id="Heading">  
      <h1>         
         <div id="Heading_tr"> <div id="Heading_tl"> <div id="Heading_br"> <div id="Heading_bl"> 
         <div id="Heading_left_top">
            DoxMentor4J<br>
         </div>
         <div id="Heading_right_top">
            $if(searchable)$
                  <input type="text" align="left" id="searchtext" name="searchtext" size="20">
                  <label for="maxhits">Max Hits</label>
                  <input type="text" align="left" id="maxhits" name="maxhits" value="100" size="4">
                  <input type="submit" value="Search" align="right" id="searchbutton" onclick="search()">
            $endif$
            $if(indexable)$
                  <div id="reindex">
                     <input value="ReIndex" type="submit"  id="indexbutton" onclick="reindex()">
                     <font size="-1">Powered by&nbsp;</font>
                     <img src="images/lucene_green_150-trans.png">
                  </div>
                  <div id="reindexing" style="display: none;">
                     <input value="Stop" type="submit"  id="stopindexbutton" onclick="stopIndex()">
                     <span id="indexstatus">
                        Indexing:- Please be patient                     
                     </span>
                  </div>
            $endif$
         </div>
         </div> </div> </div> </div>   
      </h1>   
   </div> 
   <div id="contents">
      <div id="header">
         <ul class="tabs">
            <li id="current" class="tab">
               <a href="#main" class="tablink"> Main </a>
            </li>
            <li id="tab2" class="tab">
               <a href="#search" class="tablink"> Search Results </a>
            </li>
         </ul>
      </div>	   
      <div id="contents2">
      <div id="main" class="tabcontent"> 
         <ul class="tree">
            $nodeEntries:templates/liTree()$
         </ul>
      </div>
      <div id="search" style="display: none;" class="tabcontent"> 
      </div>      
      </div>
   </div>
   <div id="load">Loading
   </div>      
</body>
</html>
