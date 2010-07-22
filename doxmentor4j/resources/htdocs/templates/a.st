<li>
$if(it.valid)$
      $if(it.targetexists)$
         <a href="$it.href$" target="$it.target$"> $it.name$ </a>
      $else$
         <a href="$it.href$"> $it.name$ </a>
      $endif$
$else$
   <font color="#C0C0C0"> $it.name$ </font>
$endif$
</li>