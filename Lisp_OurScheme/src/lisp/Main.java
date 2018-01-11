// StyleCheckType InputStreamReader
// PL101_9927202

package lisp;

import java.io.*;
import java.util.*;

class Lexeme {
  static final int LEFT_PAREN = 0;
  static final int RIGHT_PAREN = 1;
  static final int INT = 2;
  static final int STRING = 3;
  static final int DOT = 4;
  static final int FLOAT = 5;
  static final int NIL = 6;
  static final int T = 7;
  static final int QUOTE = 8;
  static final int SYMBOL = 9;
} // class Lexeme

class S_expType {
  static final int ATOM = 0;
  static final int S_EXP = 1;
  static final int FUNCTION = 2;
} // class S_expType

class Main {
  private static int sLine = 1, sCol = 0;
  private static char sTmpChar = '\0';
  private static InputStreamReader sInputStream = new InputStreamReader( System.in );
  private static Token sCurToken = null, sTmpToken = null;
  private static S_exp sCurS_exp = null;
  public static S_exp sResult = null;
  private static Vector<Binding> sTopBlock = new Vector();
  private static Vector<Vector> sStack = new Vector();
  public static boolean sIsExit = false;
  private static String sTestNum = "";
  public static void main( String[] args ) throws Throwable {
    System.out.println( "Welcome to OurScheme!" );
    sStack.add( sTopBlock );
    LoadInternalFunctions();
    for ( char ch = GetChar() ; Character.isDigit( ch ) ; sTestNum += ch, ch = GetChar() )
      ;
    for ( sLine = 1, sCol = 0 ; true ; sResult = null ) {
      try {
        System.out.print( "\n> " );
        sCurS_exp = ReadS_exp();
        ReadWhiteSpaceBehideExp();
        sResult = EvalS_exp( sCurS_exp );
        if ( sResult != null ) sResult.PrintAll( 0, false );
        System.out.println();
      } // try
      catch ( SyntaxException syntaxError ) {
        System.out.println( syntaxError.getMessage() );
        // syntaxError.printStackTrace();
        if ( !sIsExit ) {
          try {
            char ch = GetChar();
            while ( ch != '\n' ) ch = GetChar();
          } // try
          catch ( EndOfFileException endOfFileException ) {
          } // catch
        } // if
        else {
          System.out.print( "\nThanks for using OurScheme!" );
          System.exit( 0 );
        } // else
        
        sLine = 1;
        sCol = 0;
      } // catch
      catch ( EvalException evalError ) {
        System.out.print( evalError.getMessage() );
        // evalError.printStackTrace();
        if ( sResult != null ) {
          sResult.PrintAll( 0, false );
          System.out.println();
        } // if
        
        while ( sStack.size() != 2 ) sStack.removeElementAt( 0 );
      } // catch
    } // for
  } // main()

  public static char GetChar() throws Throwable {
    char ch, eof = ( char ) -1;
    if ( sTmpChar != '\0' ) {
      ch = sTmpChar;
      sTmpChar = '\0';
    } // if
    else ch = ( char ) sInputStream.read();
    if ( ch == eof ) throw new EndOfFileException();
    sCol++;
    return ch;
  } // GetChar()

  public static void UnGetChar( char ch ) {
    sTmpChar = ch;
    sCol--;
  } // UnGetChar()
    
  private static boolean IsWhiteSpace( char ch ) {
    if ( ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' ) return true;
    else return false;
  } // IsWhiteSpace()

  private static void ReadWhiteSpace() throws Throwable {
    char ch;
    for ( ch = GetChar() ; IsWhiteSpace( ch ) ; ch = GetChar() ) {
      if ( ch == '\n' ) {
        sLine++;
        sCol = 0;
      } // if
    } // for
    
    UnGetChar( ch );
  } // ReadWhiteSpace()
  
  private static void GetToken() throws Throwable {
    if ( sTmpToken != null ) {
      sCurToken = sTmpToken;
      sTmpToken = null;
    } // if
    else {
      ReadWhiteSpace();
      char ch = GetChar();   
      int type;
      String name = "" + ch;
      if ( ch == '"' ) {
        for ( ch = GetChar(), name = name + ch ; ch != '"' ; ch = GetChar(), name = name + ch )
          if ( ch == '\n' ) throw new UnexpectedCharacterException( sLine, sCol );
      } // if
      else if ( ch == ';' ) {
        for ( ch = GetChar() ; ch != '\n' ; ch = GetChar() )
          ;
        UnGetChar( ch );
        GetToken();
        return ;
      } // else if
      else if ( ch != '(' && ch != ')' && ch != '\'' ) {
        boolean stop = false;
        do {
          ch = GetChar();
          if ( !IsWhiteSpace( ch ) && ch != '(' && ch != ')' &&
               ch != '\'' && ch != ';' && ch != '"' ) name = name + ch;
          else {
            UnGetChar( ch );
            stop = true;
          } // else
        } while ( !stop ) ;
      } // else if

      if ( name.equals( "(" ) ) type = Lexeme.LEFT_PAREN;
      else if ( name.equals( ")" ) ) type = Lexeme.RIGHT_PAREN;
      else if ( name.charAt( 0 ) == '"' ) type = Lexeme.STRING;
      else if ( name.equals( "." ) ) type = Lexeme.DOT;
      else if ( name.equals( "nil" ) || name.equals( "#f" ) ) {
        name = new String( "nil" );
        type = Lexeme.NIL;
      } // else if
      else if ( name.equals( "t" ) || name.equals( "#t" ) ) {
        name = new String( "#t" );
        type = Lexeme.T;
      } // else if
      else if ( name.equals( "'" ) ) {
        name = new String( "quote" );
        type = Lexeme.QUOTE;
      } // else if
      else {
        int len = name.length();
        int dotNum = 0, specialNum = 0;
        boolean haveAlpha = false, haveDigit = false;
        for ( int i = 0 ; i < len ; i++ ) {
          if ( name.charAt( i ) == '.' ) dotNum++;
          else if ( Character.isLetter( name.charAt( i ) ) ) haveAlpha = true;
          else if ( Character.isDigit( name.charAt( i ) ) ) haveDigit = true;
          else specialNum++;
        } // for

        if ( !haveAlpha && dotNum <= 1 && haveDigit &&
             ( specialNum == 0 ||
               ( specialNum == 1 && ( name.charAt( 0 ) == '+' || name.charAt( 0 ) == '-' ) ) ) ) {
          if ( dotNum == 0 ) type = Lexeme.INT;
          else type = Lexeme.FLOAT;
        } // if
        else type = Lexeme.SYMBOL;
      } // else
      
      if ( type == Lexeme.INT && name.charAt( 0 ) == '+' ) {
        String tmp = "";
        for ( int i = 1 ; i < name.length() ; i++ )
          tmp += name.charAt( i );
        name = tmp;
      } // if

      sCurToken = new Token( type, name );
    } // else
  } // GetToken()

  private static void UnGetToken() {
    sTmpToken = sCurToken;
    sCurToken = null;
  } // UnGetToken()

  private static boolean IsPrimitives( String name ) {
    // Constructors
    if ( name.equals( "cons" ) ) return true;
    else if ( name.equals( "list" ) ) return true;

    // Bypassing the default evaluation
    else if ( name.equals( "quote" ) ) return true;

    // The binding of a symbol to an S-expression
    else if ( name.equals( "define" ) ) return true;

    else if ( name.equals( "lambda" ) ) return true;
    else if ( name.equals( "let" ) ) return true;
    else if ( name.equals( "cond" ) ) return true;

    // Part accessors
    else if ( name.equals( "car" ) ) return true;
    else if ( name.equals( "cdr" ) ) return true;

    // Primitive predicates
    else if ( name.equals( "pair?" ) ) return true;
    else if ( name.equals( "null?" ) ) return true;
    else if ( name.equals( "integer?" ) ) return true;
    else if ( name.equals( "real?" ) ) return true;
    else if ( name.equals( "number?" ) ) return true;
    else if ( name.equals( "string?" ) ) return true;
    else if ( name.equals( "boolean?" ) ) return true;
    else if ( name.equals( "symbol?" ) ) return true;

    // Basic arithmetic, logical and string operations
    else if ( name.equals( "+" ) ) return true;
    else if ( name.equals( "-" ) ) return true;
    else if ( name.equals( "*" ) ) return true;
    else if ( name.equals( "/" ) ) return true;
    else if ( name.equals( "not" ) ) return true;
    else if ( name.equals( "and" ) ) return true;
    else if ( name.equals( "or" ) ) return true;
    else if ( name.equals( ">" ) ) return true;
    else if ( name.equals( ">=" ) ) return true;
    else if ( name.equals( "<" ) ) return true;
    else if ( name.equals( "<=" ) ) return true;
    else if ( name.equals( "=" ) ) return true;
    else if ( name.equals( "string-append" ) ) return true;
    else if ( name.equals( "string>?" ) ) return true;

    // Eqivalence tester
    else if ( name.equals( "eqv?" ) ) return true;
    else if ( name.equals( "equal?" ) ) return true;

    // Sequencing and functional composition
    else if ( name.equals( "begin" ) ) return true;

    // Conditionals
    else if ( name.equals( "if" ) ) return true;
    
    else if ( name.equals( "exit" ) ) return true;
    else if ( name.equals( "clean-environment" ) ) return true;
    
    else if ( name.equals( "create-error-object" ) ) return true;
    else if ( name.equals( "error-object?" ) ) return true;
    
    else if ( name.equals( "read" ) ) return true;
    else if ( name.equals( "write" ) ) return true;
    
    else if ( name.equals( "display-string" ) ) return true;
    else if ( name.equals( "newline" ) ) return true;
    
    else if ( name.equals( "eval" ) ) return true;
    
    else if ( name.equals( "set!" ) ) return true;
    
    else return false;
  } // IsPrimitives()  
  

  
  private static S_exp ReadS_exp() throws Throwable {
    S_exp exp = new S_exp();
    GetToken();
    if ( sCurToken.GetTokenType() == Lexeme.SYMBOL ||
         sCurToken.GetTokenType() == Lexeme.INT ||
         sCurToken.GetTokenType() == Lexeme.FLOAT ||
         sCurToken.GetTokenType() == Lexeme.STRING ||
         sCurToken.GetTokenType() == Lexeme.NIL ||
         sCurToken.GetTokenType() == Lexeme.T ) {
      exp.SetAtom( sCurToken );
      return exp;
    } // if
    else if ( sCurToken.GetTokenType() == Lexeme.DOT ) {
      char ch = GetChar();
      if ( IsWhiteSpace( ch ) ) {
        if ( ch == '\n' ) throw new UnexpectedCharacterException( sLine, sCol ); 
        else throw new UnexpectedCharacterException( sLine, sCol, ch );
      } // if
      else {
        UnGetChar( ch );
        throw new UnexpectedCharacterException( sLine, sCol - sCurToken.GetTokenName().length() + 1,
                                                sCurToken.GetTokenName().charAt( 0 ) );
      } // else
    } // else if
    else if ( sCurToken.GetTokenType() == Lexeme.LEFT_PAREN ) {
      GetToken();
      if ( sCurToken.GetTokenType() == Lexeme.RIGHT_PAREN ) {
        sCurToken = new Token( Lexeme.NIL, "nil" );
        exp.SetAtom( sCurToken );
        return exp;
      } // if
      else UnGetToken();
      exp.AddS_exp( ReadS_exp() );
      while ( IsS_exp() ) exp.AddS_exp( ReadS_exp() );
      GetToken();
      if ( sCurToken.GetTokenType() == Lexeme.DOT ) {
        Token dotToken = sCurToken;
        int dotIndex = exp.GetS_expVector().size();
        exp.AddS_exp( ReadS_exp() );
        int lastIndex = exp.GetS_expVector().size() - 1;
        S_exp lastExp = exp.GetS_expVector().lastElement();
        if ( lastExp.GetAtom() == null ) {
          for ( int i = 0 ; i < lastExp.GetS_expVector().size() ; i++ )
            exp.AddS_exp( lastExp.GetS_expVector().get( i ) );
          exp.GetS_expVector().remove( lastIndex );
        } // if
        else {
          if ( exp.GetS_expVector().get( dotIndex ).GetAtom().GetTokenType() == Lexeme.NIL )
            exp.GetS_expVector().remove( dotIndex );
          else {
            S_exp dotExp = new S_exp();
            dotExp.SetAtom( dotToken );
            exp.GetS_expVector().insertElementAt( dotExp, dotIndex );
          } // else
        } // else

        GetToken();
      } // if

      if ( sCurToken.GetTokenType() != Lexeme.RIGHT_PAREN )
        throw new UnexpectedCharacterException( sLine, sCol - sCurToken.GetTokenName().length() + 1,
                                                sCurToken.GetTokenName().charAt( 0 ) );
      return exp;
    } // else if
    else if ( sCurToken.GetTokenType() == Lexeme.QUOTE ) {
      S_exp quoteExp = new S_exp();
      quoteExp.SetAtom( sCurToken );
      exp.AddS_exp( quoteExp );
      sCurToken.SetTokenType( Lexeme.SYMBOL );
      exp.AddS_exp( ReadS_exp() );
      return exp;
    } // else if
    else throw new UnexpectedCharacterException( sLine, sCol - sCurToken.GetTokenName().length() + 1,
                                                 sCurToken.GetTokenName().charAt( 0 ) );
  } // ReadS_exp()
  
  private static boolean IsS_exp() throws Throwable {
    GetToken();
    if ( sCurToken.GetTokenType() == Lexeme.SYMBOL ||
         sCurToken.GetTokenType() == Lexeme.INT ||
         sCurToken.GetTokenType() == Lexeme.FLOAT ||
         sCurToken.GetTokenType() == Lexeme.STRING ||
         sCurToken.GetTokenType() == Lexeme.NIL ||
         sCurToken.GetTokenType() == Lexeme.T ||
         sCurToken.GetTokenType() == Lexeme.LEFT_PAREN ||
         sCurToken.GetTokenType() == Lexeme.QUOTE ) {
      UnGetToken();
      return true;
    } // if
    else {
      UnGetToken();
      return false;
    } // else
  } // IsS_exp()

  private static void ReadWhiteSpaceBehideExp() throws Throwable {
    sLine = 1;
    sCol = 0;
    try {
      do {
        char ch = GetChar();
        if ( ch == '\n' || ch == ';' ) {
          while ( ch != '\n' ) ch = GetChar();
          sLine = 1;
          sCol = 0;
          return ;
        } // if
        else if ( ch != ' ' && ch != '\t' && ch != '\r' ) {
          UnGetChar( ch );
          return ;
        } // else if
      } while ( true ) ;
    } // try
    catch ( EndOfFileException endOfFileException ) {
    } // catch
  } // ReadWhiteSpaceBehideExp()
  
  private static void LoadInternalFunctions() throws Throwable {
    Vector<Binding> internalFunctions = new Vector();
    sStack.add( internalFunctions );
    internalFunctions.add( new Binding( "cons", new Function( "cons" ) ) );
    internalFunctions.add( new Binding( "list", new Function( "list" ) ) );
    
    internalFunctions.add( new Binding( "quote", new Function( "quote" ) ) );
    
    internalFunctions.add( new Binding( "define", new Function( "define" ) ) );
    
    internalFunctions.add( new Binding( "car", new Function( "car" ) ) );
    internalFunctions.add( new Binding( "cdr", new Function( "cdr" ) ) );
    
    internalFunctions.add( new Binding( "pair?", new Function( "pair?" ) ) );
    internalFunctions.add( new Binding( "null?", new Function( "null?" ) ) );
    internalFunctions.add( new Binding( "integer?", new Function( "integer?" ) ) );
    internalFunctions.add( new Binding( "real?", new Function( "real?" ) ) );
    internalFunctions.add( new Binding( "number?", new Function( "number?" ) ) );
    internalFunctions.add( new Binding( "string?", new Function( "string?" ) ) );
    internalFunctions.add( new Binding( "boolean?", new Function( "boolean?" ) ) );
    internalFunctions.add( new Binding( "symbol?", new Function( "symbol?" ) ) );
    
    internalFunctions.add( new Binding( "+", new Function( "+" ) ) );
    internalFunctions.add( new Binding( "-", new Function( "-" ) ) );
    internalFunctions.add( new Binding( "*", new Function( "*" ) ) );
    internalFunctions.add( new Binding( "/", new Function( "/" ) ) );
    internalFunctions.add( new Binding( "not", new Function( "not" ) ) );
    internalFunctions.add( new Binding( "and", new Function( "and" ) ) );
    internalFunctions.add( new Binding( "or", new Function( "or" ) ) );
    internalFunctions.add( new Binding( ">", new Function( ">" ) ) );
    internalFunctions.add( new Binding( ">=", new Function( ">=" ) ) );
    internalFunctions.add( new Binding( "<", new Function( "<" ) ) );
    internalFunctions.add( new Binding( "<=", new Function( "<=" ) ) );
    internalFunctions.add( new Binding( "=", new Function( "=" ) ) );
    internalFunctions.add( new Binding( "string-append", new Function( "string-append" ) ) );
    internalFunctions.add( new Binding( "string>?", new Function( "string>?" ) ) );
    internalFunctions.add( new Binding( "eqv?", new Function( "eqv?" ) ) );
    internalFunctions.add( new Binding( "equal?", new Function( "equal?" ) ) );
    internalFunctions.add( new Binding( "begin", new Function( "begin" ) ) );
    
    internalFunctions.add( new Binding( "if", new Function( "if" ) ) );
    internalFunctions.add( new Binding( "cond", new Function( "cond" ) ) );
    
    internalFunctions.add( new Binding( "let", new Function( "let" ) ) );
    internalFunctions.add( new Binding( "lambda", new Function( "lambda" ) ) );
    internalFunctions.add( new Binding( "exit", new Function( "exit" ) ) );
    internalFunctions.add( new Binding( "clean-environment", new Function( "clean-environment" ) ) );
    
    internalFunctions.add( new Binding( "create-error-object", new Function( "create-error-object" ) ) );
    internalFunctions.add( new Binding( "error-object?", new Function( "error-object?" ) ) );
    
    internalFunctions.add( new Binding( "read", new Function( "read" ) ) );
    internalFunctions.add( new Binding( "write", new Function( "write" ) ) );
    
    internalFunctions.add( new Binding( "display-string", new Function( "display-string" ) ) );
    internalFunctions.add( new Binding( "newline", new Function( "newline" ) ) );
    
    internalFunctions.add( new Binding( "eval", new Function( "eval" ) ) );
    internalFunctions.add( new Binding( "set!", new Function( "set!" ) ) );
  } // LoadInternalFunctions()
  
  private static S_exp EvalS_exp( S_exp exp ) throws Throwable {
    S_exp result = new S_exp();
    if ( exp.GetType() == S_expType.ATOM ) {
      if ( exp.GetAtom().GetTokenType() == Lexeme.SYMBOL )
        result = GetSymbolBinding( exp.GetAtom().GetTokenName() );
      else result = exp;
    } // if
    else {
      Vector<S_exp> expVector = exp.GetS_expVector();
      for ( int i = 0 ; i < expVector.size() ; i++ ) {
        if ( expVector.get( i ).GetType() == S_expType.ATOM &&
             expVector.get( i ).GetAtom().GetTokenType() == Lexeme.DOT )
          throw new NonListException( exp );
      } // for

      if ( expVector.get( 0 ).GetType() == S_expType.ATOM ) {
        int type = expVector.get( 0 ).GetAtom().GetTokenType();
        String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
        
        if ( type == Lexeme.SYMBOL ) {
          S_exp temp = GetSymbolBinding( functionName );
          if ( temp.GetType() != S_expType.FUNCTION )
            throw new ApplyNonFunctionException( temp );
          
          String internalFunctionName = temp.GetFunction().mInternalFunction;
          if ( internalFunctionName != null ) {
            if ( internalFunctionName.equals( "clean-environment" ) && exp != sCurS_exp )
              throw new CleanEnvironmentFormatException();
            
            if ( internalFunctionName.equals( "define" ) && exp != sCurS_exp )
              throw new DefineFormatException();
            
            if ( internalFunctionName.equals( "exit" ) && exp != sCurS_exp )
              throw new LevelOfExitException();

            if ( internalFunctionName.equals( "cons" ) )
              result = CONS( expVector );
            else if ( internalFunctionName.equals( "list" ) )
              result = LIST( expVector );
            else if ( internalFunctionName.equals( "quote" ) )
              result = QUOTE( expVector );
            else if ( internalFunctionName.equals( "define" ) )
              result = DEFINE( expVector );
            
            else if ( internalFunctionName.equals( "car" ) )
              result = CAR( expVector );
            else if ( internalFunctionName.equals( "cdr" ) )
              result = CDR( expVector );
            
            else if ( internalFunctionName.equals( "pair?" ) )
              result = IsPAIR( expVector );
            else if ( internalFunctionName.equals( "null?" ) )
              result = IsNULL( expVector );
            else if ( internalFunctionName.equals( "integer?" ) )
              result = IsINTEGER( expVector );
            else if ( internalFunctionName.equals( "real?" ) )
              result = IsREAL( expVector );
            else if ( internalFunctionName.equals( "number?" ) )
              result = IsNUMBER( expVector );
            else if ( internalFunctionName.equals( "string?" ) )
              result = IsSTRING( expVector );
            else if ( internalFunctionName.equals( "boolean?" ) )
              result = IsBOOLEAN( expVector );
            else if ( internalFunctionName.equals( "symbol?" ) )
              result = IsSYMBOL( expVector );
            
            else if ( internalFunctionName.equals( "+" ) )
              result = PLUS( expVector );
            else if ( internalFunctionName.equals( "-" ) )
              result = MINUS( expVector );
            else if ( internalFunctionName.equals( "*" ) )
              result = MUL( expVector );
            else if ( internalFunctionName.equals( "/" ) )
              result = DIV( expVector );
            else if ( internalFunctionName.equals( "not" ) )
              result = NOT( expVector );
            else if ( internalFunctionName.equals( "and" ) )
              result = AND( expVector );
            else if ( internalFunctionName.equals( "or" ) )
              result = OR( expVector );
            else if ( internalFunctionName.equals( ">" ) )
              result = MORE( expVector );
            else if ( internalFunctionName.equals( ">=" ) )
              result = MORE_EQUAL( expVector );
            else if ( internalFunctionName.equals( "<" ) )
              result = LESS( expVector );
            else if ( internalFunctionName.equals( "<=" ) )
              result = LESS_EQUAL( expVector );
            else if ( internalFunctionName.equals( "=" ) )
              result = EQU( expVector );
            
            else if ( internalFunctionName.equals( "string-append" ) )
              result = STRING_APPEND( expVector );
            else if ( internalFunctionName.equals( "string>?" ) )
              result = STRING_COMPARE( expVector );
            
            else if ( internalFunctionName.equals( "eqv?" ) )
              result = EQV( expVector );
            else if ( internalFunctionName.equals( "equal?" ) )
              result = EQUAL( expVector );
            
            else if ( internalFunctionName.equals( "begin" ) )
              result = BEGIN( expVector );
            
            else if ( internalFunctionName.equals( "if" ) )
              result = IF( expVector );
            else if ( internalFunctionName.equals( "cond" ) )
              result = COND( expVector );
            
            else if ( internalFunctionName.equals( "let" ) )
              result = LET( expVector );
            else if ( internalFunctionName.equals( "lambda" ) )
              result = LAMBDA( expVector );
            else if ( internalFunctionName.equals( "exit" ) )
              EXIT( expVector );
            else if ( internalFunctionName.equals( "clean-environment" ) )
              result = CLEAN_ENVIRONMENT( expVector );
            
            else if ( internalFunctionName.equals( "create-error-object" ) )
              result = CREATE_ERROR_OBJECT( expVector );
            else if ( internalFunctionName.equals( "error-object?" ) )
              result = IsERROR_OBJECT( expVector );
            else if ( internalFunctionName.equals( "read" ) )
              result = READ( expVector );
            else if ( internalFunctionName.equals( "write" ) )
              result = WRITE( expVector );
            else if ( internalFunctionName.equals( "display-string" ) )
              result = DISPLAY_STRING( expVector );
            else if ( internalFunctionName.equals( "newline" ) )
              result = NEWLINE( expVector );
            else if ( internalFunctionName.equals( "eval" ) )
              result = EVAL( expVector );
            else if ( internalFunctionName.equals( "set!" ) )
              result = SET( expVector );
          } // if
          else {
            Function function = temp.GetFunction();
            if ( function.mParameter.size() != expVector.size() - 1 )
              throw new IncorrectNumberOfArgumentsException( functionName );
            
            Vector<Binding> newBlock = new Vector();
            for ( int i = 0 ; i < function.mParameter.size() ; i++ ) {
              result = EvalS_exp( expVector.get( i + 1 ) );
              AddSymbolBinding( function.mParameter.get( i ), result, newBlock );
            } // for

            sStack.insertElementAt( newBlock, 0 );
            for ( int i = 0 ; i < function.mS_expVector.size() ; i++ )
              result = EvalS_exp( function.mS_expVector.get( i ) );
            sStack.removeElementAt( 0 );
          } // else
        } // if
        else throw new ApplyNonFunctionException( expVector.get( 0 ) );
      } // if
      else if ( expVector.get( 0 ).GetType() == S_expType.S_EXP ) {
        S_exp temp = EvalS_exp( expVector.get( 0 ) );
        if ( temp.GetType() != S_expType.FUNCTION )
          throw new ApplyNonFunctionException( temp );
        expVector.setElementAt( temp, 0 );
        result = EvalS_exp( exp );
      } // else if
      else {
        Function function = expVector.get( 0 ).GetFunction();
        String functionName = "lambda expression";
        if ( function.mParameter.size() != expVector.size() - 1 )
          throw new IncorrectNumberOfArgumentsException( functionName );
        
        Vector<Binding> newBlock = new Vector();
        for ( int i = 0 ; i < function.mParameter.size() ; i++ ) {
          result = EvalS_exp( expVector.get( i + 1 ) );
          AddSymbolBinding( function.mParameter.get( i ), result, newBlock );
        } // for
        
        sStack.insertElementAt( newBlock, 0 );
        for ( int i = 0 ; i < function.mS_expVector.size() ; i++ )
          result = EvalS_exp( function.mS_expVector.get( i ) );
        sStack.removeElementAt( 0 );
      } // else
    } // else

    return result;
  } // EvalS_exp()

  private static void AddSymbolBinding( String name, S_exp binding, Vector<Binding> curBlock ) {
    if ( curBlock == null ) curBlock = sStack.get( 0 );
    for ( int i = 0 ; i < curBlock.size() ; i++ ) {
      if ( curBlock.get( i ).mName.equals( name ) ) {
        curBlock.get( i ).mS_exp = binding;
        return ;
      } // if
    } // for

    Binding newBinding = new Binding( name, binding );
    curBlock.add( newBinding );
    return ;
  } // AddSymbolBinding()

  private static S_exp GetSymbolBinding( String name ) throws Throwable {
    for ( int i = 0 ; i < sStack.size() ; i++ ) {
      Vector<Binding> curBlock = sStack.get( i );
      for ( int j = 0 ; j < curBlock.size() ; j++ ) {
        if ( curBlock.get( j ).mName.equals( name ) )
          return curBlock.get( j ).mS_exp;
      } // for
    } // for
    
    throw new UnboundSymbolException( name );
  } // GetSymbolBinding()
  
  private static S_exp CONS( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 3 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    Vector<S_exp> resultVector = new Vector();
    for ( int i = 1 ; i < expVector.size() ; i++ ) {
      S_exp evalResult = EvalS_exp( expVector.get( i ) );
      resultVector.add( evalResult );
    } // for
    
    if ( resultVector.get( 1 ).GetType() == S_expType.ATOM &&
         resultVector.get( 1 ).GetAtom().GetTokenType() == Lexeme.NIL )
      resultVector.removeElementAt( 1 );
    else if ( resultVector.get( 1 ).GetType() == S_expType.S_EXP ) {
      Vector<S_exp> temp = resultVector.get( 1 ).GetS_expVector();
      resultVector.removeElement( resultVector.get( 1 ) );
      for ( int i = 0 ; i < temp.size() ; i++ )
        resultVector.add( temp.get( i ) );
    } // else if
    else {
      S_exp dotAtom = new S_exp();
      dotAtom.SetAtom( new Token( Lexeme.DOT, "." ) );
      resultVector.insertElementAt( dotAtom, 1 );
    } // else

    S_exp newS_exp = new S_exp();
    newS_exp.SetS_expVector( resultVector );
    return newS_exp;
  } // CONS()

  private static S_exp LIST( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() < 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    Vector<S_exp> resultVector = new Vector();
    for ( int i = 1 ; i < expVector.size() ; i++ ) {
      S_exp evalResult = EvalS_exp( expVector.get( i ) );
      resultVector.add( evalResult );
    } // for
    
    S_exp newS_exp = new S_exp();
    newS_exp.SetS_expVector( resultVector );
    return newS_exp;
  } // LIST()
  
  private static S_exp QUOTE( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    return expVector.get( 1 );
  } // QUOTE()
  
  private static S_exp DEFINE( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() < 3 )
      throw new DefineFormatException();
    
    String name;
    if ( expVector.get( 1 ).GetType() == S_expType.ATOM ) {
      name = expVector.get( 1 ).GetAtom().GetTokenName();
      if ( expVector.size() > 3 ||
           expVector.get( 1 ).GetAtom().GetTokenType() != Lexeme.SYMBOL ||
           IsPrimitives( name ) ) {
        throw new DefineFormatException();
      } // if
      
      AddSymbolBinding( name, EvalS_exp( expVector.get( 2 ) ), null );
    } // if
    else {
      if ( expVector.get( 1 ).GetS_expVector().size() < 1 )
        throw new DefineFormatException();
      
      Vector<S_exp> temp = expVector.get( 1 ).GetS_expVector();
      for ( int i = 0 ; i < temp.size() ; i++ ) {
        if ( temp.get( i ).GetType() != S_expType.ATOM || 
             temp.get( i ).GetAtom().GetTokenType() != Lexeme.SYMBOL ||
             IsPrimitives( temp.get( i ).GetAtom().GetTokenName() ) )
          throw new DefineFormatException();
      } // for
      
      name = temp.get( 0 ).GetAtom().GetTokenName();
      Vector<String> parameter = new Vector();
      for ( int i = 1 ; i < temp.size() ; i++ )
        parameter.add( temp.get( i ).GetAtom().GetTokenName() );
      
      Vector<S_exp> newExpVector = new Vector();
      for ( int i = 2 ; i < expVector.size() ; i++ )
        newExpVector.add( expVector.get( i ) );

      S_exp newS_exp = new S_exp();
      newS_exp.SetFunction( new Function( parameter, newExpVector ) );
      AddSymbolBinding( name, newS_exp, null );
    } // else
    
    System.out.print( name + " defined" );
    return null;
  } // DEFINE()

  private static S_exp COND( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() < 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    boolean haveAns = false;
    S_exp evalResult = null;
    
    for ( int i = 1 ; i < expVector.size() ; i++ ) {
      if ( expVector.get( i ).GetType() != S_expType.S_EXP ||
           expVector.get( i ).GetS_expVector().size() < 2 )
        throw new ParameterFormatException( functionName, expVector.get( i ) );
    } // for
    
    for ( int i = 1 ; !haveAns && i < expVector.size() ; i++ ) {
      S_exp cond = EvalS_exp( expVector.get( i ).GetS_expVector().get( 0 ) );
      if ( cond.GetType() == S_expType.ATOM ) {
        if ( cond.GetAtom().GetTokenType() != Lexeme.NIL ) {
          for ( int j = 1 ; j < expVector.get( i ).GetS_expVector().size() ; j++ )
            evalResult = EvalS_exp( expVector.get( i ).GetS_expVector().get( j ) );
          haveAns = true;
        } // if
      } // if
      else {
        for ( int j = 1 ; j < expVector.get( i ).GetS_expVector().size() ; j++ )
          evalResult = EvalS_exp( expVector.get( i ).GetS_expVector().get( j ) );
        haveAns = true;
      } // else
    } // for
    
    if ( haveAns ) return evalResult;
    else throw new NoReturnValueException( functionName );
  } // COND()

  private static S_exp LAMBDA( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() < 3 )
      throw new LambdaFormatException();
    
    if ( expVector.get( 1 ).GetType() == S_expType.ATOM &&
         expVector.get( 1 ).GetAtom().GetTokenType() != Lexeme.NIL )
      throw new LambdaFormatException();
    
    Vector<String> parameterName = new Vector();
    Vector<S_exp> newExpVector = new Vector();
    if ( expVector.get( 1 ).GetType() == S_expType.S_EXP ) {
      Vector<S_exp> parameter = expVector.get( 1 ).GetS_expVector();
      for ( int i = 0 ; i < parameter.size() ; i++ ) {
        if ( parameter.get( i ).GetType() == S_expType.S_EXP ||
             parameter.get( i ).GetAtom().GetTokenType() != Lexeme.SYMBOL ||
             IsPrimitives( parameter.get( i ).GetAtom().GetTokenName() ) )
          throw new LambdaFormatException();
        parameterName.add( parameter.get( i ).GetAtom().GetTokenName() );
      } // for
    } // if
    
    for ( int i = 2 ; i < expVector.size() ; i++ )
      newExpVector.add( expVector.get( i ) );
    
    S_exp newS_exp = new S_exp();
    newS_exp.SetFunction( new Function( parameterName, newExpVector ) );
    return newS_exp;
  } // LAMBDA()
  
  private static S_exp LET( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() < 3 )
      throw new LetFormatException();
    
    if ( expVector.get( 1 ).GetType() == S_expType.ATOM &&
         expVector.get( 1 ).GetAtom().GetTokenType() != Lexeme.NIL )
      throw new LetFormatException();
    
    Vector<Binding> newBlock = new Vector();
    if ( expVector.get( 1 ).GetType() == S_expType.S_EXP ) {
      Vector<S_exp> localDefine = expVector.get( 1 ).GetS_expVector();
      for ( int i = 0 ; i < localDefine.size() ; i++ ) {
        S_exp localSymbol = localDefine.get( i );
        if ( localSymbol.GetType() != S_expType.S_EXP ||
             localSymbol.GetS_expVector().size() != 2 ||
             localSymbol.GetS_expVector().get( 0 ).GetType() != S_expType.ATOM ||
             localSymbol.GetS_expVector().get( 0 ).GetAtom().GetTokenType() != Lexeme.SYMBOL ||
             IsPrimitives( localSymbol.GetS_expVector().get( 0 ).GetAtom().GetTokenName() ) )
          throw new LetFormatException();
      } // for
      
      for ( int i = 0 ; i < localDefine.size() ; i++ ) {
        S_exp localSymbol = localDefine.get( i );
        String name = localSymbol.GetS_expVector().get( 0 ).GetAtom().GetTokenName();
        S_exp binding = EvalS_exp( localSymbol.GetS_expVector().get( 1 ) ) ;
        AddSymbolBinding( name, binding, newBlock );
      } // for
    } // if

    sStack.insertElementAt( newBlock, 0 );
    S_exp evalResult = null;
    for ( int i = 2 ; i < expVector.size() ; i++ )
      evalResult = EvalS_exp( expVector.get( i ) );
    sStack.removeElementAt( 0 );
    return evalResult;
  } // LET()

  private static S_exp CAR( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) );
    if ( evalResult.GetType() == S_expType.S_EXP )
      return evalResult.GetS_expVector().get( 0 );
    else throw new IncorrectArgumentTypeException( functionName, evalResult );
  } // CAR()
  
  private static S_exp CDR( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) );
    if ( evalResult.GetType() == S_expType.S_EXP ) {
      S_exp newS_exp = new S_exp();
      Vector<S_exp> newS_expVector = new Vector();
      newS_exp.SetS_expVector( newS_expVector );
      for ( int i = 0 ; i < evalResult.GetS_expVector().size() ; i++ )
        newS_expVector.add( evalResult.GetS_expVector().get( i ) );
      
      if ( newS_expVector.size() > 1 ) {
        newS_exp.GetS_expVector().removeElementAt( 0 );
        if ( newS_expVector.get( 0 ).GetType() == S_expType.ATOM &&
             newS_expVector.get( 0 ).GetAtom().GetTokenType() == Lexeme.DOT ) {
          newS_expVector.removeElementAt( 0 );
          if ( newS_expVector.size() == 1 ) newS_exp = newS_expVector.get( 0 );
        } // if
      } // if
      else newS_exp.SetAtom( new Token( Lexeme.NIL, "nil" ) );
      return newS_exp;
    } // if
    else throw new IncorrectArgumentTypeException( functionName, evalResult );
  } // CDR()

  private static S_exp IsPAIR( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    Token token;
    if ( evalResult.GetType() == S_expType.S_EXP ) token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // IsPAIR()
  
  private static S_exp IsNULL( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    Token token;
    if ( evalResult.GetType() == S_expType.ATOM &&
         evalResult.GetAtom().GetTokenType() == Lexeme.NIL )
      token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // IsNULL()
  
  private static S_exp IsINTEGER( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    Token token;
    if ( evalResult.GetType() == S_expType.ATOM &&
         evalResult.GetAtom().GetTokenType() == Lexeme.INT )
      token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // IsINTEGER()
  
  private static S_exp IsREAL( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    Token token;
    if ( evalResult.GetType() == S_expType.ATOM &&
         evalResult.GetAtom().GetTokenType() == Lexeme.FLOAT )
      token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // IsREAL()
  
  private static S_exp IsNUMBER( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    Token token;
    if ( evalResult.GetType() == S_expType.ATOM &&
         ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ||
           evalResult.GetAtom().GetTokenType() == Lexeme.FLOAT ) )
      token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // IsNUMBER()
  
  private static S_exp IsSTRING( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    Token token;
    if ( evalResult.GetType() == S_expType.ATOM &&
         evalResult.GetAtom().GetTokenType() == Lexeme.STRING )
      token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // IsSTRING()
  
  private static S_exp IsBOOLEAN( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    Token token;
    if ( evalResult.GetType() == S_expType.ATOM &&
         ( evalResult.GetAtom().GetTokenType() == Lexeme.T ||
           evalResult.GetAtom().GetTokenType() == Lexeme.NIL ) )
      token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // IsBOOLEAN()
  
  private static S_exp IsSYMBOL( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    Token token;
    if ( evalResult.GetType() == S_expType.ATOM &&
         evalResult.GetAtom().GetTokenType() == Lexeme.SYMBOL )
      token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // IsSYMBOL()

  private static S_exp PLUS( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() < 3 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    int type = Lexeme.INT;
    double ans = 0;
    S_exp evalResult = null, newS_exp = new S_exp();
    for ( int i = 1 ; i < expVector.size() ; i++ ) {
      evalResult = EvalS_exp( expVector.get( i ) );
      if ( evalResult.GetType() == S_expType.ATOM &&
           ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ||
             evalResult.GetAtom().GetTokenType() == Lexeme.FLOAT ) ) {
        if ( evalResult.GetAtom().GetTokenType() == Lexeme.INT )
          ans += Integer.parseInt( evalResult.GetAtom().GetTokenName() );
        else {
          type = Lexeme.FLOAT;
          ans += Double.parseDouble( evalResult.GetAtom().GetTokenName() );
        } // else
      } // if
      else throw new IncorrectArgumentTypeException( functionName, evalResult );
    } // for    
    
    Token token;
    if ( type == Lexeme.INT )
      token = new Token( type, Integer.toString( ( ( int ) ans ) ) );
    else token = new Token( type, Double.toString( ans ) );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // PLUS()
  
  private static S_exp MINUS( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() < 3 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    int type = Lexeme.INT;
    double ans = 0;
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    if ( evalResult.GetType() == S_expType.ATOM &&
         ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ||
           evalResult.GetAtom().GetTokenType() == Lexeme.FLOAT ) ) {
      if ( evalResult.GetAtom().GetTokenType() == Lexeme.INT )
        ans = Integer.parseInt( evalResult.GetAtom().GetTokenName() );
      else {
        type = Lexeme.FLOAT;
        ans = Double.parseDouble( evalResult.GetAtom().GetTokenName() );
      } // else
    } // if
    else throw new IncorrectArgumentTypeException( functionName, evalResult );
    
    for ( int i = 2 ; i < expVector.size() ; i++ ) {
      evalResult = EvalS_exp( expVector.get( i ) );     
      if ( evalResult.GetType() == S_expType.ATOM &&
           ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ||
             evalResult.GetAtom().GetTokenType() == Lexeme.FLOAT ) ) {
        if ( evalResult.GetAtom().GetTokenType() == Lexeme.INT )
          ans -= Integer.parseInt( evalResult.GetAtom().GetTokenName() );
        else {
          type = Lexeme.FLOAT;
          ans -= Double.parseDouble( evalResult.GetAtom().GetTokenName() );
        } // else if
      } // if
      else throw new IncorrectArgumentTypeException( functionName, evalResult );
    } // for
    
    Token token;
    if ( type == Lexeme.INT )
      token = new Token( type, Integer.toString( ( int ) ans ) );
    else token = new Token( type, Double.toString( ans ) );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // MINUS()
  
  private static S_exp MUL( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() < 3 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    int type = Lexeme.INT;
    double ans = 1;
    S_exp evalResult = null, newS_exp = new S_exp();
    for ( int i = 1 ; i < expVector.size() ; i++ ) {
      evalResult = EvalS_exp( expVector.get( i ) );
      if ( evalResult.GetType() == S_expType.ATOM &&
           ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ||
             evalResult.GetAtom().GetTokenType() == Lexeme.FLOAT ) ) {
        if ( evalResult.GetAtom().GetTokenType() == Lexeme.INT )
          ans *= Integer.parseInt( evalResult.GetAtom().GetTokenName() );
        else {
          type = Lexeme.FLOAT;
          ans *= Double.parseDouble( evalResult.GetAtom().GetTokenName() );
        } // else
      } // if
      else throw new IncorrectArgumentTypeException( functionName, evalResult );
    } // for
    
    Token token;
    if ( type == Lexeme.INT )
      token = new Token( type, Integer.toString( ( int ) ans ) );
    else token = new Token( type, Double.toString( ans ) );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // MUL()
  
  private static S_exp DIV( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() < 3 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    int type = Lexeme.INT;
    double ans = 0;
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    if ( evalResult.GetType() == S_expType.ATOM &&
         ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ||
           evalResult.GetAtom().GetTokenType() == Lexeme.FLOAT ) ) {
      if ( evalResult.GetAtom().GetTokenType() == Lexeme.INT )
        ans = Integer.parseInt( evalResult.GetAtom().GetTokenName() );
      else {
        type = Lexeme.FLOAT;
        ans = Double.parseDouble( evalResult.GetAtom().GetTokenName() );
      } // else
    } // if
    else throw new IncorrectArgumentTypeException( functionName, evalResult );

    for ( int i = 2 ; i < expVector.size() ; i++ ) {
      evalResult = EvalS_exp( expVector.get( i ) );
      if ( evalResult.GetType() == S_expType.ATOM &&
           ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ||
             evalResult.GetAtom().GetTokenType() == Lexeme.FLOAT ) ) {
        if ( evalResult.GetAtom().GetTokenType() == Lexeme.INT )
          ans /= Integer.parseInt( evalResult.GetAtom().GetTokenName() );
        else {
          type = Lexeme.FLOAT;
          ans /= Double.parseDouble( evalResult.GetAtom().GetTokenName() );
        } // else
      } // if
      else throw new IncorrectArgumentTypeException( functionName, evalResult );
    } // for
    
    Token token;
    if ( type == Lexeme.INT )
      token = new Token( type, Integer.toString( ( int ) ans ) );
    else token = new Token( type, Double.toString( ans ) );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // DIV()
  
  private static S_exp NOT( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    boolean ans = false;
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    if ( evalResult.GetType() == S_expType.ATOM &&
         evalResult.GetAtom().GetTokenType() == Lexeme.NIL ) ans = true;
    
    Token token;
    if ( ans ) token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // NOT()
  
  private static S_exp AND( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 3 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    boolean ans1 = false, ans2 = false;
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    if ( evalResult.GetType() == S_expType.ATOM &&
         ( evalResult.GetAtom().GetTokenType() == Lexeme.T ||
           evalResult.GetAtom().GetTokenType() == Lexeme.NIL ) ) {
      if ( evalResult.GetAtom().GetTokenType() == Lexeme.T ) ans1 = true;
    } // if
    else ans1 = true;
  
    evalResult = EvalS_exp( expVector.get( 2 ) );
    if ( evalResult.GetType() == S_expType.ATOM &&
         ( evalResult.GetAtom().GetTokenType() == Lexeme.T ||
           evalResult.GetAtom().GetTokenType() == Lexeme.NIL ) ) {
      if ( evalResult.GetAtom().GetTokenType() == Lexeme.T ) ans2 = true;
    } // if
    else ans2 = true;
    
    Token token;
    if ( ans1 && ans2 ) token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // AND()

  private static S_exp OR( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 3 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    boolean ans1 = false, ans2 = false;
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    if ( evalResult.GetType() == S_expType.ATOM &&
         ( evalResult.GetAtom().GetTokenType() == Lexeme.T ||
           evalResult.GetAtom().GetTokenType() == Lexeme.NIL ) ) {
      if ( evalResult.GetAtom().GetTokenType() == Lexeme.T ) ans1 = true;
    } // if
    else ans1 = true;

    evalResult = EvalS_exp( expVector.get( 2 ) );
    if ( evalResult.GetType() == S_expType.ATOM &&
         ( evalResult.GetAtom().GetTokenType() == Lexeme.T ||
           evalResult.GetAtom().GetTokenType() == Lexeme.NIL ) ) {
      if ( evalResult.GetAtom().GetTokenType() == Lexeme.T ) ans2 = true;
    } // if
    else ans2 = true; 

    Token token;
    if ( ans1 || ans2 ) token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // OR()

  private static S_exp MORE( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() < 3 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    double num1 = 0, num2 = 0;
    boolean ans = true;
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    if ( evalResult.GetType() == S_expType.ATOM &&
         ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ||
           evalResult.GetAtom().GetTokenType() == Lexeme.FLOAT ) ) {
      if ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ) 
        num1 = Integer.parseInt( evalResult.GetAtom().GetTokenName() );
      else num1 = Double.parseDouble( evalResult.GetAtom().GetTokenName() );
    } // if
    else throw new IncorrectArgumentTypeException( functionName, evalResult );
    
    for ( int i = 2 ; i < expVector.size() ; i++ ) {
      evalResult = EvalS_exp( expVector.get( i ) );
      if ( evalResult.GetType() == S_expType.ATOM &&
           ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ||
             evalResult.GetAtom().GetTokenType() == Lexeme.FLOAT ) ) {
        if ( evalResult.GetAtom().GetTokenType() == Lexeme.INT )
          num2 = Integer.parseInt( evalResult.GetAtom().GetTokenName() );
        else num2 = Double.parseDouble( evalResult.GetAtom().GetTokenName() );
      } // if
      else throw new IncorrectArgumentTypeException( functionName, evalResult );
      
      if ( ans ) {
        if ( num1 > num2 ) num1 = num2;
        else ans = false;
      } // if
    } // for
    
    Token token;
    if ( ans ) token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // MORE()

  private static S_exp MORE_EQUAL( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() < 3 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    double num1 = 0, num2 = 0;
    boolean ans = true;
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    if ( evalResult.GetType() == S_expType.ATOM &&
         ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ||
           evalResult.GetAtom().GetTokenType() == Lexeme.FLOAT ) ) {
      if ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ) 
        num1 = Integer.parseInt( evalResult.GetAtom().GetTokenName() );
      else num1 = Double.parseDouble( evalResult.GetAtom().GetTokenName() );
    } // if
    else throw new IncorrectArgumentTypeException( functionName, evalResult );
    
    for ( int i = 2 ; i < expVector.size() ; i++ ) {
      evalResult = EvalS_exp( expVector.get( i ) );
      if ( evalResult.GetType() == S_expType.ATOM &&
           ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ||
             evalResult.GetAtom().GetTokenType() == Lexeme.FLOAT ) ) {
        if ( evalResult.GetAtom().GetTokenType() == Lexeme.INT )
          num2 = Integer.parseInt( evalResult.GetAtom().GetTokenName() );
        else num2 = Double.parseDouble( evalResult.GetAtom().GetTokenName() );
      } // if
      else throw new IncorrectArgumentTypeException( functionName, evalResult );
      
      if ( ans ) {
        if ( num1 >= num2 ) num1 = num2;
        else ans = false;
      } // if
    } // for
    
    Token token;
    if ( ans ) token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // MORE_EQUAL()
  
  private static S_exp LESS( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() < 3 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    double num1 = 0, num2 = 0;
    boolean ans = true;
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    if ( evalResult.GetType() == S_expType.ATOM &&
         ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ||
           evalResult.GetAtom().GetTokenType() == Lexeme.FLOAT ) ) {
      if ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ) 
        num1 = Integer.parseInt( evalResult.GetAtom().GetTokenName() );
      else num1 = Double.parseDouble( evalResult.GetAtom().GetTokenName() );
    } // if
    else throw new IncorrectArgumentTypeException( functionName, evalResult );
    
    for ( int i = 2 ; i < expVector.size() ; i++ ) {
      evalResult = EvalS_exp( expVector.get( i ) );
      if ( evalResult.GetType() == S_expType.ATOM &&
           ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ||
             evalResult.GetAtom().GetTokenType() == Lexeme.FLOAT ) ) {
        if ( evalResult.GetAtom().GetTokenType() == Lexeme.INT )
          num2 = Integer.parseInt( evalResult.GetAtom().GetTokenName() );
        else num2 = Double.parseDouble( evalResult.GetAtom().GetTokenName() );
      } // if
      else throw new IncorrectArgumentTypeException( functionName, evalResult );
    
      if ( ans ) {
        if ( num1 < num2 ) num1 = num2;
        else ans = false;
      } // if
    } // for
    
    Token token;
    if ( ans ) token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // LESS()
  
  private static S_exp LESS_EQUAL( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() < 3 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    double num1 = 0, num2 = 0;
    boolean ans = true;
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    if ( evalResult.GetType() == S_expType.ATOM &&
         ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ||
           evalResult.GetAtom().GetTokenType() == Lexeme.FLOAT ) ) {
      if ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ) 
        num1 = Integer.parseInt( evalResult.GetAtom().GetTokenName() );
      else num1 = Double.parseDouble( evalResult.GetAtom().GetTokenName() );
    } // if
    else throw new IncorrectArgumentTypeException( functionName, evalResult );
    
    for ( int i = 2 ; i < expVector.size() ; i++ ) {
      evalResult = EvalS_exp( expVector.get( i ) );
      if ( evalResult.GetType() == S_expType.ATOM &&
           ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ||
             evalResult.GetAtom().GetTokenType() == Lexeme.FLOAT ) ) {
        if ( evalResult.GetAtom().GetTokenType() == Lexeme.INT )
          num2 = Integer.parseInt( evalResult.GetAtom().GetTokenName() );
        else num2 = Double.parseDouble( evalResult.GetAtom().GetTokenName() );
      } // if
      else throw new IncorrectArgumentTypeException( functionName, evalResult );
      
      if ( ans ) {
        if ( num1 <= num2 ) num1 = num2;
        else ans = false;
      } // if
    } // for
    
    Token token;
    if ( ans ) token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // LESS_EQUAL()
  
  private static S_exp EQU( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() < 3 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    double num1 = 0, num2 = 0;
    boolean ans = true;
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    if ( evalResult.GetType() == S_expType.ATOM &&
         ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ||
           evalResult.GetAtom().GetTokenType() == Lexeme.FLOAT ) ) {
      if ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ) 
        num1 = Integer.parseInt( evalResult.GetAtom().GetTokenName() );
      else num1 = Double.parseDouble( evalResult.GetAtom().GetTokenName() );
    } // if
    else throw new IncorrectArgumentTypeException( functionName, evalResult );
    
    for ( int i = 2 ; i < expVector.size() ; i++ ) {
      evalResult = EvalS_exp( expVector.get( i ) );
      if ( evalResult.GetType() == S_expType.ATOM &&
           ( evalResult.GetAtom().GetTokenType() == Lexeme.INT ||
             evalResult.GetAtom().GetTokenType() == Lexeme.FLOAT ) ) {
        if ( evalResult.GetAtom().GetTokenType() == Lexeme.INT )
          num2 = Integer.parseInt( evalResult.GetAtom().GetTokenName() );
        else num2 = Double.parseDouble( evalResult.GetAtom().GetTokenName() );
      } // if
      else throw new IncorrectArgumentTypeException( functionName, evalResult );
      
      if ( ans ) {
        if ( num1 == num2 ) num1 = num2;
        else ans = false;
      } // if
    } // for
    
    Token token;
    if ( ans ) token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // EQU()
  
  private static S_exp STRING_APPEND( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() < 3 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    String str = "\"";
    S_exp evalResult = null, newS_exp = new S_exp();
    for ( int i = 1 ; i < expVector.size() ; i++ ) {
      evalResult = EvalS_exp( expVector.get( i ) );
      if ( evalResult.GetType() == S_expType.ATOM &&
           evalResult.GetAtom().GetTokenType() == Lexeme.STRING ) {
        for ( int j = 1 ; j < evalResult.GetAtom().GetTokenName().length()-1 ; j++ )
          str += evalResult.GetAtom().GetTokenName().charAt( j );
      } // if
      else throw new IncorrectArgumentTypeException( functionName, evalResult );
    } // for

    str += '"';
    Token token = new Token( Lexeme.STRING, str );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // STRING_APPEND()
  
  private static S_exp STRING_COMPARE( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() < 3 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    String str1 = "", str2 = "";
    boolean ans = true;
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    if ( evalResult.GetType() == S_expType.ATOM &&
         evalResult.GetAtom().GetTokenType() == Lexeme.STRING ) {
      for ( int j = 1 ; j < evalResult.GetAtom().GetTokenName().length()-1 ; j++ )
        str1 += evalResult.GetAtom().GetTokenName().charAt( j );
    } // if
    else throw new IncorrectArgumentTypeException( functionName, evalResult );
    
    for ( int i = 2 ; i < expVector.size() ; i++ ) {
      evalResult = EvalS_exp( expVector.get( i ) );
      if ( evalResult.GetType() == S_expType.ATOM &&
           evalResult.GetAtom().GetTokenType() == Lexeme.STRING ) {
        str2 = "";
        for ( int j = 1 ; j < evalResult.GetAtom().GetTokenName().length()-1 ; j++ )
          str2 += evalResult.GetAtom().GetTokenName().charAt( j );
      } // if
      else throw new IncorrectArgumentTypeException( functionName, evalResult );
      
      if ( ans ) {
        if ( str1.compareTo( str2 ) > 0 ) str1 = str2;
        else ans = false;
      } // if
    } // for

    Token token;
    if ( ans ) token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // STRING_COMPARE()
  
  private static S_exp EQV( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 3 )
      throw new IncorrectNumberOfArgumentsException( functionName );

    boolean ans = true;
    S_exp evalResult1 = EvalS_exp( expVector.get( 1 ) ), 
          evalResult2 = EvalS_exp( expVector.get( 2 ) ),
          newS_exp = new S_exp();
    if ( evalResult1 == evalResult2 ) ans = true;
    else if ( evalResult1.GetType() == S_expType.ATOM &&
              evalResult2.GetType() == S_expType.ATOM &&
              evalResult1.GetAtom().GetTokenType() != Lexeme.STRING &&
              evalResult1.GetAtom().GetTokenType() == evalResult2.GetAtom().GetTokenType() &&
              evalResult1.GetAtom().GetTokenName().equals( evalResult2.GetAtom().GetTokenName() ) )
      ans = true;
    else ans = false;

    Token token;
    if ( ans ) token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // EQV()
  
  private static boolean CompareS_exp( S_exp exp1, S_exp exp2 ) {
    if ( exp1.GetType() == S_expType.ATOM &&
         exp2.GetType() == S_expType.ATOM &&
         exp1.GetAtom().GetTokenType() == exp2.GetAtom().GetTokenType() &&
         exp1.GetAtom().GetTokenName().equals( exp2.GetAtom().GetTokenName() ) )
      return true;
    else if ( exp1.GetType() == S_expType.S_EXP &&
              exp2.GetType() == S_expType.S_EXP &&
              exp1.GetS_expVector().size() == exp2.GetS_expVector().size() ) {
      boolean ans = true;
      S_exp temp1, temp2;
      for ( int i = 0 ; i < exp1.GetS_expVector().size() ; i++ ) {
        temp1 = exp1.GetS_expVector().get( i );
        temp2 = exp2.GetS_expVector().get( i );
        if ( ans ) ans = CompareS_exp( temp1, temp2 );
      } // for
        
      return ans;
    } // else if
    else if ( exp1.GetType() == S_expType.FUNCTION &&
              exp2.GetType() == S_expType.FUNCTION &&
              exp1.GetFunction() == exp2.GetFunction() ) return true;
    else return false;
  } // CompareS_exp()
  
  private static S_exp EQUAL( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 3 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    S_exp evalResult1 = EvalS_exp( expVector.get( 1 ) ),
          evalResult2 = EvalS_exp( expVector.get( 2 ) ),
          newS_exp = new S_exp();
    boolean ans = CompareS_exp( evalResult1, evalResult2 );
    Token token;
    if ( ans ) token = new Token( Lexeme.T, "#t" );
    else token = new Token( Lexeme.NIL, "nil" );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // EQUAL()
  
  private static S_exp BEGIN( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() < 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    S_exp evalResult = null;
    for ( int i = 1 ; i < expVector.size() ; i++ )
      evalResult = EvalS_exp( expVector.get( i ) );
    return evalResult;
  } // BEGIN()
  
  private static S_exp IF( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 4 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    S_exp cond = EvalS_exp( expVector.get( 1 ) ), evalResult = null;
    if ( cond.GetType() == S_expType.ATOM &&
         cond.GetAtom().GetTokenType() == Lexeme.NIL ) 
      evalResult = EvalS_exp( expVector.get( 3 ) );
    else evalResult = EvalS_exp( expVector.get( 2 ) );
    return evalResult;
  } // IF()
  
  private static void EXIT( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 1 )
      throw new IncorrectNumberOfArgumentsException( functionName );

    System.out.print( "\nThanks for using OurScheme!" );
    System.exit( 0 );
  } // EXIT()
  
  private static S_exp CLEAN_ENVIRONMENT( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 1 )
      throw new IncorrectNumberOfArgumentsException( functionName );

    sTopBlock.removeAllElements();
    System.out.print( "environment cleaned" );
    return null;
  } // CLEAN_ENVIRONMENT()
  
  private static S_exp CREATE_ERROR_OBJECT( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    if ( expVector.get( 1 ).GetType() != S_expType.ATOM ||
         expVector.get( 1 ).GetAtom().GetTokenType() != Lexeme.STRING )
      throw new IncorrectArgumentTypeException( functionName, expVector.get( 1 ) );
    
    S_exp newS_exp = new S_exp();
    Token token = new Token( Lexeme.STRING, expVector.get( 1 ).GetAtom().GetTokenName() );
    token.SetIsErrorObject( true );
    newS_exp.SetAtom( token );
    return newS_exp;
  } // CREATE_ERROR_OBJECT()
  
  private static S_exp IsERROR_OBJECT( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) ), newS_exp = new S_exp();
    boolean ans = false;
    if ( evalResult.GetType() == S_expType.ATOM && 
         evalResult.GetAtom().GetIsErrorObject() )
      ans = true;
    
    if ( ans ) newS_exp.SetAtom( new Token( Lexeme.T, "#t" ) );
    else newS_exp.SetAtom( new Token( Lexeme.NIL, "nil" ) );
    return newS_exp;
  } // IsERROR_OBJECT()
  
  private static S_exp READ( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 1 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    S_exp newS_exp;
    try {
      newS_exp = ReadS_exp();
      ReadWhiteSpaceBehideExp();
    } // try
    catch ( SyntaxException syntaxError ) {
      newS_exp = new S_exp();
      newS_exp.SetAtom( new Token( Lexeme.STRING, "\"" + syntaxError.getMessage() + "\"" ) );
      newS_exp.GetAtom().SetIsErrorObject( true );
      
      if ( !sIsExit ) {
        try {
          char ch = GetChar();
          while ( ch != '\n' ) ch = GetChar();
        } // try
        catch ( EndOfFileException endOfFileException ) {
          UnGetChar( ( char ) -1 );
        } // catch
      } // if
      else sIsExit = false;
      
      sLine = 1;
      sCol = 0;
    } // catch
    
    return newS_exp;
  } // READ()
  
  private static S_exp WRITE( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) );
    evalResult.PrintAll( 0, false );
    return evalResult;
  } // WRITE()
  
  private static S_exp DISPLAY_STRING( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    S_exp evalResult = EvalS_exp( expVector.get( 1 ) );
    if ( evalResult.GetType() != S_expType.ATOM ||
         evalResult.GetAtom().GetTokenType() != Lexeme.STRING )
      throw new IncorrectArgumentTypeException( functionName, evalResult );
    
    String str = evalResult.GetAtom().GetTokenName();
    for ( int i = 1 ; i < str.length() - 1 ; i++ )
      System.out.print( str.charAt( i ) );
    return evalResult;
  } // DISPLAY_STRING()
  
  private static S_exp NEWLINE( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 1 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    System.out.println();
    S_exp newS_exp = new S_exp();
    newS_exp.SetAtom( new Token( Lexeme.NIL, "nil" ) );
    return newS_exp;
  } // NEWLINE()
  
  private static S_exp EVAL( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 2 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    S_exp evalResult = EvalS_exp( EvalS_exp( expVector.get( 1 ) ) );
    return evalResult;
  } // EVAL()

  private static S_exp SET( Vector<S_exp> expVector ) throws Throwable {
    String functionName = expVector.get( 0 ).GetAtom().GetTokenName();
    if ( expVector.size() != 3 )
      throw new IncorrectNumberOfArgumentsException( functionName );
    
    String name = expVector.get( 1 ).GetAtom().GetTokenName();
    if ( expVector.get( 1 ).GetType() != S_expType.ATOM || 
         expVector.get( 1 ).GetAtom().GetTokenType() != Lexeme.SYMBOL ||
         IsPrimitives( name ) )
      throw new IncorrectArgumentTypeException( functionName, expVector.get( 1 ) );
    
    AddSymbolBinding( name, EvalS_exp( expVector.get( 2 ) ), null );

    S_exp newS_exp = new S_exp();
    newS_exp.SetAtom( new Token( Lexeme.NIL, "nil" ) );
    return newS_exp;
  } // SET()
} // class Main

class Token {
  private int mType;
  private String mName;
  private boolean mIsErrorObject;
  
  public Token( int type, String name ) {
    mType = type;
    mName = name;
    mIsErrorObject = false;
  } // Token()

  public void SetTokenType( int type ) {
    mType = type;
  } // SetTokenType()

  public void SetTokenName( String name ) {
    mName = name;
  } // SetTokenName()

  public void SetIsErrorObject( boolean isErrorObject ) {
    mIsErrorObject = isErrorObject;
  } // SetIsErrorObject()
  
  public int GetTokenType() {
    return mType;
  } // GetTokenType()

  public String GetTokenName() {
    return mName;
  } // GetTokenName()

  public boolean GetIsErrorObject() {
    return mIsErrorObject;
  } // GetIsErrorObject()
} // class Token

class S_exp {
  private int mType;
  private Token mAtom;
  private Vector<S_exp> mS_expVector;
  private Function mFunction;

  public S_exp() {
    mAtom = null;
    mS_expVector = null;
    mFunction = null;
  } // S_exp()
  
  public void AddS_exp( S_exp exp ) {
    if ( mS_expVector == null ) {
      mType = S_expType.S_EXP; 
      mS_expVector = new Vector();
    } // if
    
    mS_expVector.add( exp );
  } // AddS_exp()
  
  public int GetType() {
    return mType;
  } // GetType()
  
  public Token GetAtom() {
    return mAtom;
  } // GetAtom()
  
  public Vector<S_exp> GetS_expVector() {
    return mS_expVector;
  } // GetS_expVector()
  
  public Function GetFunction() {
    return mFunction;
  } // GetFunction()
  
  public void SetAtom( Token token ) {
    mType = S_expType.ATOM;
    mAtom = token;
    mS_expVector = null;
    mFunction = null;
  } // SetAtom()
  
  public void SetS_expVector( Vector<S_exp> expVector ) {
    mType = S_expType.S_EXP;
    mS_expVector = expVector;
    mAtom = null;
    mFunction = null;
  } // SetS_expVector()
  
  public void SetFunction( Function func ) {
    mType = S_expType.FUNCTION;
    mFunction = func;
    mAtom = null;
    mS_expVector = null;
  } // SetFunction()
  
  public void PrintAll( int spaceNum, boolean printSpace ) {
    if ( printSpace )
      for ( int i = 0 ; i < spaceNum ; i++ )
        System.out.print( " " );
    
    if ( mType == S_expType.ATOM ) {
      if ( mAtom.GetTokenType() == Lexeme.FLOAT )
        System.out.printf( "%.3f", Double.parseDouble( mAtom.GetTokenName() ) );
      else System.out.print( mAtom.GetTokenName() );
    } // if
    else if ( mType == S_expType.FUNCTION ) System.out.print( "#function" );
    else {
      System.out.print( "( " );
      for ( int i = 0 ; i < mS_expVector.size() ; i++ ) {
        if ( i != 0 ) printSpace = true;
        else printSpace = false;
        mS_expVector.get( i ).PrintAll( spaceNum + 2, printSpace );
        System.out.println();
      } // for

      for ( int i = 0 ; i < spaceNum ; i++ )
        System.out.print( " " );
      System.out.print( ")" );
    } // else
  } // PrintAll()
} // class S_exp

class Binding {
  public String mName;
  public S_exp mS_exp;
  public Binding( String name, S_exp exp ) {
    mName = name;
    mS_exp = exp;
  } // Binding()
  
  public Binding( String name, Function func ) {
    mName = name;
    mS_exp = new S_exp();
    mS_exp.SetFunction( func );
  } // Binding()
} // class Binding

class Function {
  public String mInternalFunction;
  public Vector<String> mParameter;
  public Vector<S_exp> mS_expVector;
  public Function( Vector<String> parameter, Vector<S_exp> expVector ) {
    mInternalFunction = null;
    mParameter = parameter;
    mS_expVector = expVector;
  } // Function()
  
  public Function( String internalFunction ) {
    mInternalFunction = internalFunction;
    mParameter = null;
    mS_expVector = null;
  } // Function()
} // class Function

class SyntaxException extends Exception {
  public SyntaxException( String errorMessage ) {
    super( errorMessage );
  } // SyntaxException()
} // class SyntaxException

class EndOfFileException extends SyntaxException {
  public EndOfFileException() {
    super( "ERROR : END-OF-FILE encountered when there should be more input" );
    Main.sIsExit = true;
  } // EndOfFileException()
} // class EndOfFileException

class UnexpectedCharacterException extends SyntaxException {
  public UnexpectedCharacterException( int line, int column, char errorChar ) {
    super( "ERROR (unexpected character) : line " + line + " column " + column +
           " character '" + errorChar + "'" );
  } // UnexpectedCharacterException()
  
  public UnexpectedCharacterException( int line, int column ) {
    super( "ERROR (unexpected character) : line " + line + " column " + column +
           " LINE-ENTER encountered" );
    Main.UnGetChar( '\n' );
  } // UnexpectedCharacterException()
} // class UnexpectedCharacterException

class EvalException extends Exception {
  public EvalException( String errorMessage ) {
    super( errorMessage );
  } // EvalException()
} // class EvalException

class NonListException extends EvalException {
  public NonListException( S_exp exp ) {
    super( "ERROR (non-list) : " );
    Main.sResult = exp;
  } // NonListException()
} // class NonListException

class ApplyNonFunctionException extends EvalException {
  public ApplyNonFunctionException( S_exp exp ) {
    super( "ERROR (attempt to apply non-function) : " );
    Main.sResult = exp;
  } // ApplyNonFunctionException()
} // class ApplyNonFunctionException

class CleanEnvironmentFormatException extends EvalException {
  public CleanEnvironmentFormatException() {
    super( "ERROR (clean-environment format)\n" );
  } // CleanEnvironmentFormatException()
} // class CleanEnvironmentFormatException

class DefineFormatException extends EvalException {
  public DefineFormatException() {
    super( "ERROR (define format)\n" );
  } // DefineFormatException()
} // class DefineFormatException

class LambdaFormatException extends EvalException {
  public LambdaFormatException() {
    super( "ERROR (lambda format)\n" );
  } // LambdaFormatException()
} // class LambdaFormatException

class LetFormatException extends EvalException {
  public LetFormatException() {
    super( "ERROR (LET format)\n" );
  } // LetFormatException()
} // class LetFormatException

class LevelOfExitException extends EvalException {
  public LevelOfExitException() {
    super( "ERROR (level of exit)\n" );
  } // LevelOfExitException()
} // class LevelOfExitException

class UnboundSymbolException extends EvalException {
  public UnboundSymbolException( String name ) {
    super( "ERROR (unbound symbol) : " + name + "\n" );
  } // UnboundSymbolException()
} // class UnboundSymbolException

class IncorrectNumberOfArgumentsException extends EvalException {
  public IncorrectNumberOfArgumentsException( String functionName ) {
    super( "ERROR (incorrect number of arguments) : " + functionName + "\n" );
  } // IncorrectNumberOfArgumentsException()
} // class IncorrectNumberOfArgumentsException

class IncorrectArgumentTypeException extends EvalException {
  public IncorrectArgumentTypeException( String functionName, S_exp exp ) {
    super( "ERROR (" + functionName + " with incorrect argument type) : " );
    Main.sResult = exp;
  } // IncorrectArgumentTypeException()
} // class IncorrectArgumentTypeException

class ParameterFormatException extends EvalException {
  public ParameterFormatException( String functionName, S_exp exp ) {
    super( "ERROR (" + functionName + " parameter format) : " );
    Main.sResult = exp;
  } // ParameterFormatException()
} // class ParameterFormatException

class NoReturnValueException extends EvalException {
  public NoReturnValueException( String functionName ) {
    super( "ERROR (no return value) : " + functionName + "\n" );
  } // NoReturnValueException()
} // class NoReturnValueException

